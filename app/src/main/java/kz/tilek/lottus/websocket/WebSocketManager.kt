// ./app/src/main/java/kz/tilek/lottus/websocket/WebSocketManager.kt
package kz.tilek.lottus.websocket

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.* // Импорт корутин
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kz.tilek.lottus.App
import kz.tilek.lottus.R
import kz.tilek.lottus.api.ApiClient
import kz.tilek.lottus.api.BidMessage
import kz.tilek.lottus.data.TokenManager
import kz.tilek.lottus.models.Notification
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import ua.naiksoftware.stomp.dto.StompHeader
import java.util.concurrent.TimeUnit // Для задержки

object WebSocketManager {

    private const val TAG = "WebSocketManager"
    private const val NOTIFICATION_CHANNEL_ID = "lottus_notifications"
    private var mStompClient: StompClient? = null
    private val compositeDisposable = CompositeDisposable()
    private val activeSubscriptions = mutableMapOf<String, Disposable>()
    private val gson = Gson()

    // --- Новые переменные для переподключения ---
    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob()) // Корутин скоуп для менеджера
    private var reconnectJob: Job? = null // Job для отслеживания попыток переподключения
    private var isManuallyDisconnected = false // Флаг, что отключение было выполнено вручную (logout)
    private const val INITIAL_RECONNECT_DELAY_MS = 5000L // Начальная задержка 5 секунд
    private const val MAX_RECONNECT_DELAY_MS = 60000L // Максимальная задержка 1 минута
    private var currentReconnectDelay = INITIAL_RECONNECT_DELAY_MS
    // -------------------------------------------

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    val bidMessagesFlow = MutableStateFlow<BidMessage?>(null)
    val notificationMessagesFlow = MutableStateFlow<Notification?>(null)

    enum class ConnectionState { CONNECTING, CONNECTED, DISCONNECTED, ERROR }

    fun connect() {
        // Если уже подключаемся или подключены, выходим
        if (_connectionState.value == ConnectionState.CONNECTING || _connectionState.value == ConnectionState.CONNECTED) {
            Log.d(TAG, "Уже подключаемся или подключен.")
            return
        }

        // Отменяем предыдущие попытки переподключения, если они были
        reconnectJob?.cancel()
        reconnectJob = null
        isManuallyDisconnected = false // Сбрасываем флаг ручного отключения
        currentReconnectDelay = INITIAL_RECONNECT_DELAY_MS // Сбрасываем задержку

        val token = TokenManager.token
        if (token == null) {
            Log.e(TAG, "Невозможно подключиться: JWT токен отсутствует.")
            _connectionState.value = ConnectionState.ERROR
            // Не пытаемся переподключиться, если нет токена
            return
        }

        val headers = listOf(StompHeader("Authorization", "Bearer $token"))

        // --- Добавляем Heartbeat ---
        val currentClient: StompClient
        if (Build.MODEL == "sdk_gphone64_x86_64") {
            currentClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, "ws://10.0.2.2:8080/ws/websocket")
                .withServerHeartbeat(30000)
                .withClientHeartbeat(30000)
        } else {
            currentClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, ApiClient.WEBSOCKET_URL)
                .withServerHeartbeat(30000)
                .withClientHeartbeat(30000)
        }

        mStompClient = currentClient
            // Отправляем heartbeat каждые 30 секунд, ожидаем от сервера каждые 30 секунд
            // Сервер тоже должен быть настроен на похожие значения!

        // -------------------------

        compositeDisposable.clear() // Очищаем предыдущие подписки перед новым подключением

        val lifecycleDisposable = mStompClient?.lifecycle()
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe({ lifecycleEvent ->
                if (currentClient != mStompClient && lifecycleEvent.type != LifecycleEvent.Type.CLOSED && lifecycleEvent.type != LifecycleEvent.Type.ERROR) {
                    Log.w(TAG, "Получено событие lifecycle от устаревшего клиента. Игнорируем.")
                    return@subscribe // Игнорируем событие от старого клиента (кроме закрытия/ошибки)
                }
                when (lifecycleEvent.type) {
                    LifecycleEvent.Type.OPENED -> {
                        Log.d(TAG, "Соединение установлено!")
                        _connectionState.value = ConnectionState.CONNECTED
                        currentReconnectDelay = INITIAL_RECONNECT_DELAY_MS // Сброс задержки при успехе
                        subscribeToUserNotifications() // Подписываемся на уведомления
                        // Повторно подписываемся на топики, которые были активны (если нужно сохранять их список)
                        // resubscribeToActiveTopics() // (Реализация этой функции зависит от логики приложения)
                    }
                    LifecycleEvent.Type.CLOSED -> {
                        Log.d(TAG, "Соединение закрыто.")
                        if (currentClient == mStompClient || mStompClient == null) {
                            _connectionState.value = ConnectionState.DISCONNECTED
                            clearSubscriptions()
                            if (!isManuallyDisconnected) {
                                scheduleReconnect()
                            }
                        } else {
                            Log.d(TAG, "Получено CLOSED от старого клиента. Игнорируем.")
                        }
                        // ----------------------------------------------------------
                    }
                    LifecycleEvent.Type.ERROR -> {
                        Log.e(TAG, "Ошибка соединения: ", lifecycleEvent.exception)
                        if (currentClient == mStompClient || mStompClient == null) {
                            _connectionState.value = ConnectionState.ERROR
                            clearSubscriptions()
                            if (!isManuallyDisconnected) {
                                scheduleReconnect()
                            }
                        } else {
                            Log.d(TAG, "Получено ERROR от старого клиента. Игнорируем.")
                        }
                        // ----------------------------------------------------------
                    }
                    LifecycleEvent.Type.FAILED_SERVER_HEARTBEAT -> {
                        Log.w(TAG, "Пропущен heartbeat сервера.")
                        // Соединение, скорее всего, разорвано, ожидаем CLOSED или ERROR
                    }
                }
            }, { throwable ->
                Log.e(TAG, "Ошибка подписки на lifecycle: ", throwable)
                if (currentClient == mStompClient || mStompClient == null) {
                    _connectionState.value = ConnectionState.ERROR
                    clearSubscriptions()
                    if (!isManuallyDisconnected) {
                        scheduleReconnect()
                    }
                }
                // ----------------------------------------------------------
            })
        compositeDisposable.add(lifecycleDisposable!!)

        Log.d(TAG, "Попытка подключения к ${ApiClient.WEBSOCKET_URL}...")
        _connectionState.value = ConnectionState.CONNECTING
        mStompClient?.connect(headers)
    }

    fun disconnect() {
        Log.d(TAG, "Ручное отключение...")
        isManuallyDisconnected = true
        reconnectJob?.cancel()
        reconnectJob = null

        val clientToDisconnect = mStompClient // Сохраняем ссылку на текущий клиент
        mStompClient = null // Сразу обнуляем глобальную ссылку

        compositeDisposable.clear() // Отписываемся от всего

        clientToDisconnect?.disconnect() // Отключаем сохраненный клиент

        _connectionState.value = ConnectionState.DISCONNECTED
        clearSubscriptions() // Очистка активных подписок (на всякий случай)
        Log.d(TAG, "Ручное отключение завершено.")
    }

    // --- Новая функция для планирования переподключения ---
    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) {
            // Уже запланировано или выполняется
            return
        }
        reconnectJob = managerScope.launch {
            Log.d(TAG, "Планируем переподключение через ${currentReconnectDelay / 1000} сек...")
            delay(currentReconnectDelay)
            currentReconnectDelay = (currentReconnectDelay * 2).coerceAtMost(MAX_RECONNECT_DELAY_MS)

            // Проверяем состояние ПЕРЕД попыткой
            if (_connectionState.value != ConnectionState.CONNECTED && !isManuallyDisconnected) {
                Log.d(TAG, "Выполняем попытку переподключения...")
                // --- Добавляем проверку, что другой connect не запущен ---
                if (_connectionState.value == ConnectionState.CONNECTING) {
                    Log.w(TAG, "Попытка переподключения пропущена, уже идет подключение.")
                } else {
                    withContext(Dispatchers.Main) {
                        connect()
                    }
                }
            } else {
                Log.d(TAG, "Переподключение отменено (уже подключено или отключено вручную).")
            }
        }
    }
    // ----------------------------------------------------

    fun subscribeToTopic(topicPath: String) {
        // ... (код подписки остается прежним, но теперь он будет вызван после успешного connect или reconnect)
        if (mStompClient == null || mStompClient?.isConnected != true) {
            Log.w(TAG, "Невозможно подписаться на $topicPath: нет соединения.")
            return
        }
        if (activeSubscriptions.containsKey(topicPath)) {
            Log.d(TAG, "Уже подписан на $topicPath")
            return
        }

        Log.d(TAG, "Подписка на топик: $topicPath")
        val topicDisposable = mStompClient?.topic(topicPath)
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe({ stompMessage ->
                Log.d(TAG, "Получено сообщение из $topicPath: ${stompMessage.payload}")
                try {
                    val bidMessage = gson.fromJson(stompMessage.payload, BidMessage::class.java)
                    bidMessagesFlow.value = bidMessage
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка парсинга BidMessage из $topicPath: ", e)
                }
            }, { throwable ->
                Log.e(TAG, "Ошибка подписки на $topicPath: ", throwable)
                activeSubscriptions.remove(topicPath)
            })

        if (topicDisposable != null) {
            compositeDisposable.add(topicDisposable)
            activeSubscriptions[topicPath] = topicDisposable
        }
    }

    private fun subscribeToUserNotifications() {
        val userId = TokenManager.userId
        val userQueuePath = "$userId/queue/notifications"
        if (mStompClient == null || mStompClient?.isConnected != true) {
            Log.w(TAG, "Невозможно подписаться на $userQueuePath: нет соединения.")
            return
        }
        if (activeSubscriptions.containsKey(userQueuePath)) {
            Log.d(TAG, "Уже подписан на $userQueuePath")
            return
        }

        Log.d(TAG, "Подписка на очередь уведомлений: $userQueuePath")
        val notificationDisposable = mStompClient?.topic(userQueuePath)
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe({ stompMessage ->
                Log.d(TAG, "Получено уведомление: ${stompMessage.payload}")
                try {
                    val notification = gson.fromJson(stompMessage.payload, Notification::class.java)
                    notificationMessagesFlow.value = notification
                    showSystemNotification(App.appContext, notification)
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка парсинга Notification из $userQueuePath: ", e)
                }
            }, { throwable ->
                Log.e(TAG, "Ошибка подписки на $userQueuePath: ", throwable)
                activeSubscriptions.remove(userQueuePath)
            })

        if (notificationDisposable != null) {
            compositeDisposable.add(notificationDisposable)
            activeSubscriptions[userQueuePath] = notificationDisposable
        }
    }

    private fun showSystemNotification(context: Context, notification: Notification) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Уведомления Lottus",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Уведомления от аукциона Lottus"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Убедись, что иконка есть
            .setContentTitle("Lottus Аукцион")
            .setContentText(notification.message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(notification.id.hashCode(), builder.build())
    }

    fun unsubscribeFromTopic(topicPath: String) {
        activeSubscriptions.remove(topicPath)?.let {
            if (!it.isDisposed) {
                Log.d(TAG, "Отписка от $topicPath")
                it.dispose()
                // НЕ удаляем из compositeDisposable здесь, он очищается при disconnect или новом connect
            }
        }
    }

    private fun clearSubscriptions() {
        // Отписываемся от всего, КРОМЕ lifecycle подписки (она в compositeDisposable)
        // Копируем ключи, чтобы избежать ConcurrentModificationException
        val topicsToUnsubscribe = activeSubscriptions.keys.toList()
        topicsToUnsubscribe.forEach { topic ->
            activeSubscriptions.remove(topic)?.dispose()
        }
        activeSubscriptions.clear()
        Log.d(TAG, "Все активные подписки на топики очищены.")
    }

    // Опционально: метод для переподписки на нужные топики после реконнекта
    // private fun resubscribeToActiveTopics() {
    //    val currentTopics = activeSubscriptions.keys.toList() // Получаем список топиков, на которые были подписаны
    //    activeSubscriptions.clear() // Очищаем старые Disposables
    //    currentTopics.forEach { subscribeToTopic(it) } // Подписываемся заново
    // }
}

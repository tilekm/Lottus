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
import kotlinx.coroutines.* 
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
import java.util.concurrent.TimeUnit 
object WebSocketManager {
    private const val TAG = "WebSocketManager"
    private const val NOTIFICATION_CHANNEL_ID = "lottus_notifications"
    private var mStompClient: StompClient? = null
    private val compositeDisposable = CompositeDisposable()
    private val activeSubscriptions = mutableMapOf<String, Disposable>()
    private val gson = Gson()
    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob()) 
    private var reconnectJob: Job? = null 
    private var isManuallyDisconnected = false 
    private const val INITIAL_RECONNECT_DELAY_MS = 5000L 
    private const val MAX_RECONNECT_DELAY_MS = 60000L 
    private var currentReconnectDelay = INITIAL_RECONNECT_DELAY_MS
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState
    val bidMessagesFlow = MutableStateFlow<BidMessage?>(null)
    val notificationMessagesFlow = MutableStateFlow<Notification?>(null)
    enum class ConnectionState { CONNECTING, CONNECTED, DISCONNECTED, ERROR }
    fun connect() {
        if (_connectionState.value == ConnectionState.CONNECTING || _connectionState.value == ConnectionState.CONNECTED) {
            Log.d(TAG, "Уже подключаемся или подключен.")
            return
        }
        reconnectJob?.cancel()
        reconnectJob = null
        isManuallyDisconnected = false 
        currentReconnectDelay = INITIAL_RECONNECT_DELAY_MS 
        val token = TokenManager.token
        if (token == null) {
            Log.e(TAG, "Невозможно подключиться: JWT токен отсутствует.")
            _connectionState.value = ConnectionState.ERROR
            return
        }
        val headers = listOf(StompHeader("Authorization", "Bearer $token"))
        val currentClient: StompClient
            currentClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, ApiClient.WEBSOCKET_URL)
                .withServerHeartbeat(30000)
                .withClientHeartbeat(30000)
        mStompClient = currentClient
        compositeDisposable.clear() 
        val lifecycleDisposable = mStompClient?.lifecycle()
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe({ lifecycleEvent ->
                if (currentClient != mStompClient && lifecycleEvent.type != LifecycleEvent.Type.CLOSED && lifecycleEvent.type != LifecycleEvent.Type.ERROR) {
                    Log.w(TAG, "Получено событие lifecycle от устаревшего клиента. Игнорируем.")
                    return@subscribe 
                }
                when (lifecycleEvent.type) {
                    LifecycleEvent.Type.OPENED -> {
                        Log.d(TAG, "Соединение установлено!")
                        _connectionState.value = ConnectionState.CONNECTED
                        currentReconnectDelay = INITIAL_RECONNECT_DELAY_MS 
                        subscribeToUserNotifications() 
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
                    }
                    LifecycleEvent.Type.FAILED_SERVER_HEARTBEAT -> {
                        Log.w(TAG, "Пропущен heartbeat сервера.")
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
        val clientToDisconnect = mStompClient 
        mStompClient = null 
        compositeDisposable.clear() 
        clientToDisconnect?.disconnect() 
        _connectionState.value = ConnectionState.DISCONNECTED
        clearSubscriptions() 
        Log.d(TAG, "Ручное отключение завершено.")
    }
    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) {
            return
        }
        reconnectJob = managerScope.launch {
            Log.d(TAG, "Планируем переподключение через ${currentReconnectDelay / 1000} сек...")
            delay(currentReconnectDelay)
            currentReconnectDelay = (currentReconnectDelay * 2).coerceAtMost(MAX_RECONNECT_DELAY_MS)
            if (_connectionState.value != ConnectionState.CONNECTED && !isManuallyDisconnected) {
                Log.d(TAG, "Выполняем попытку переподключения...")
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
    fun subscribeToTopic(topicPath: String) {
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
            .setSmallIcon(R.drawable.ic_notification) 
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
            }
        }
    }
    private fun clearSubscriptions() {
        val topicsToUnsubscribe = activeSubscriptions.keys.toList()
        topicsToUnsubscribe.forEach { topic ->
            activeSubscriptions.remove(topic)?.dispose()
        }
        activeSubscriptions.clear()
        Log.d(TAG, "Все активные подписки на топики очищены.")
    }
}

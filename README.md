# DeepSeek SDK for Kotlin Multiplatform

## 深度求索第三方库

### 内置的简单调用法

#### SimpleRequest方法

```kotlin
class SimpleRequest(
    private val apiKey: String,
    private val model: Model = Model.Chat,
    private val messages: Messages
)
```

或者还有一种构造：

```kotlin
 class SimpleRequest(
    val apiKey: String,
    val model: Model = Model.Chat,
    val messages: Messages
)
```

可以使用以下示例代码调用：

```kotlin
fun `test simple request`() {
    runBlocking {  // 协程
        SimpleRequest(
            apiKey = "Your API Key",  // 填入你的API Key
            model = Model.Chat,  // 选择要使用的模型
            messages = Messages {  // 用最炫酷的方法构建你的Message列表
                system("你是一个深索")
                user("你好，你是谁")
            }
        ).chatCompletions().collect { print(it.second ?: "") }
    }
}
```

而最令人震惊的自然是，像这样炫酷的构建方法还有好几个！

```kotlin
val messages = Messages(
    Message.System("你是一个深索"),
    Message.User("你好，你是谁")
)
```

```kotlin
val messages = Messages {
    system("你是一个深索")
    user("你好，你是谁")
}
```

```kotlin
val messages = Messages()
    .system("你是一个深索")
    .user("你好，你是谁")
```

```kotlin
val messages = Messages {
    system("你是一个深索")
}.user("你好，你是谁")
```

```kotlin
val messages = Messages()
messages.append(Message.System("你是一个深索"))
messages.append(Message.User("你好，你是谁"))
```

```kotlin
var messages = Messages()
messages = messages.system("你是一个深索")
messages = messages.user("你好，你是谁")
```

```kotlin
val messages = listOf(
    Message.System("你是一个深索"),
    Message.User("你好，你是谁")
).toMessages()
```

> 以上暂时只展示这几个，更多的留给大家慢慢探索！

让我们回归正题，解释Simple Request的调用。

首先它必须在携程中启动(问题不大反正编辑器会提示你的)。

```kotlin
fun `test simple request`() {
    runBlocking {  // 协程
        // ...
    }
}
```

然后填入参数，其中主要是API Key、模型以及Message列表：

```kotlin
val sq = SimpleRequest(
    apiKey = "Your API Key",  // 填入你的API Key
    model = Model.Chat,  // 选择要使用的模型
    messages = Messages {  // 用最炫酷的方法构建你的Message列表
        system("你是一个深索")
        user("你好，你是谁")
    }
)
```

最后调用chatCompletions方法，并使用collect方法来处理返回的流数据：

```kotlin
sq.chatCompletions().collect { print(it.second ?: "") }
```

`SimpleRequest.chatCompletions`方法返回的为`Flow<Pair<String?, String?>>`，其中first为思维链(若没有则忽视它！)
，second为模型输出的本文。
为什么要如此设计呢？为了simple(手动滑稽)，问题不大哈，因为有更加规范和灵活的方法。我推荐那个，如果你想的话可以跳过简单调用法的这几篇。
除此之外如果你不想要流式输出，那么你可以使用`SimpleRequest.chatCompletion`方法，它返回的为`Pair<String?, String?>`。

#### SimpleClient方法

这是另一个简单的内置方法，更适合同时多个聊天，更适合服务器端使用。

我们从SimpleRequest的源码出发吧！

```kotlin
class SimpleRequest(
    apiKey: String,
    private val model: Model = Model.Chat,
    private val messages: Messages
) {
    constructor(
        apiKey: String,
        model: Model = Model.Chat,
        messages: List<Message>
    ) : this(apiKey, model, messages.toMessages())

    private val client = SimpleClient(apiKey)

    suspend fun chatCompletion(): Pair<String?, String?> = client.chatCompletion(model, messages)
    fun chatCompletions(): Flow<Pair<String?, String?>> = client.chatCompletions(model, messages)
}
```

不难注意到这一行：

```kotlin
private val client = SimpleClient(apiKey)
```

这边是现在要介绍的类——`SimpleClient`了！
通过其可以避免多次创建新类，并反复传递API Key。
用起来也很简单！

这样就能创建一个`SimpleClient`实例了：

```kotlin
val client = SimpleClient(apiKey)
```

然后就可以使用它来调用`chatCompletion`和`chatCompletions`方法了：

### 高级炫酷调用法

然后就是比较深入的使用方法了，我们用到的是一个名为`Client`的类。
类似的，让我们从`SimpleClient`的源码出发

```kotlin
class SimpleClient(apiKey: String) {
    private val client = Client {
        this.apiKey = ApiKey.Key(apiKey)
    }

    suspend fun chatCompletion(
        model: Model = Model.Chat, messages: Messages
    ): Pair<String?, String?> {
        client.chatCompletion {
            this.model = model
            this.messages = messages
        }.let { res ->
            when (res) {
                is ClientMessage.Success -> {
                    return Pair(res.choices.first().message.reasoningContent, res.choices.first().message.content)
                }

                else -> {
                    return Pair("<|ERROR|>", null)
                }
            }
        }
    }

    fun chatCompletions(
        model: Model = Model.Chat,
        messages: Messages
    ): Flow<Pair<String?, String?>> = flow {
        client.chatCompletionChunk {
            this.model = model
            this.messages = messages
        }.collect { res ->
            when (res) {
                is ClientMessage.Success -> {
                    emit(Pair(res.choices.first().message.reasoningContent, res.choices.first().message.content))
                }

                is ClientMessage.Finish -> {
                    emit(Pair("<|FINISH|>", null))
                }

                is ClientMessage.Error -> {
                    emit(Pair("<|ERROR|>", null))
                }
            }
        }
    }
}
```

看起来很复杂是吧（不愧是我写的！），但别担心，我们来简单地介绍一下。
注意力惊人的你一定注意到了这一行：

```kotlin
private val client = Client {
    this.apiKey = ApiKey.Key(apiKey)
}
```

在这里我们用一种很炫酷的方式创建了`Client`。
较为简单的，我们可以用以下代码：

```kotlin
@Test
fun `test client chunk reasoning`() {
    val client = Client {
        apiKey = ApiKey.Key("Your Key")
    }
    runBlocking {  // 协程
        client.chatCompletionChunk {  // 这里就是我们的ClientMessage
            model = Model.Reasoner  // 选择模型
            messages = Messages {  // 构建Message
                user("你好")
            }
        }.collect { res ->  // 输出流
            when (res) {
                ClientMessage.Error -> {  // 错误处理
                    error("<|ERROR|>")
                }
                ClientMessage.Finish -> {  // 结束处理
                    println("<|FINISHED|>")
                }
                is ClientMessage.Success -> {  // 正常处理
                    println(res)
                }
            }
        }
    }
}
```

这里的`ClientMessage`就是我们的输出流，它有3种情况：

1. `ClientMessage.Success`：表示一次成功的输出，它有复杂的结构。
2. `ClientMessage.Finish`：表示结束，它没有带任何参数与结构。
3. `ClientMessage.Error`：表示错误，它没有带任何参数与结构。

我们来讲述一下`ClientMessage.Success`的结构。
一般的，我们只需要这样使用：

```kotlin
val reasoning = res.choices.first().message.reasoningContent  // 思维链
val content = res.choices.first().message.content  // 文本
```

至于其具体结构……太复杂了，但是总之是完全符合DeepSeek官方的设定的！
你可以参考源码：

```kotlin
sealed class ClientMessage {
    data class Success(
        val id: String,
        val `object`: Object,
        val created: Int,
        val model: String,
        val choices: List<Choice> = listOf(),
        val usage: Usage = Usage(),
        val systemFingerprint: String
    ) : ClientMessage()

    data object Finish : ClientMessage()

    data object Error : ClientMessage()

    enum class Object {
        ChatCompletion,
        ChatCompletionChunk,
    }

    data class Choice(
        val index: Int,
        val message: ChatMessage,
        val logprobs: Logprobs? = null,
        val finishReason: FinishReason? = null
    )

    data class ChatMessage(
        val role: ChatRole,
        val content: String?,
        val reasoningContent: String?,
        val toolCalls: List<ToolCall> = listOf()
    )

    enum class ChatRole {
        System, User, Assistant,
    }

    data class ToolCall(
        val id: String,
        val type: ToolType,
        val function: ToolFunction,
    )

    enum class ToolType {
        Function
    }

    data class ToolFunction(
        val name: String, val arguments: String
    )

    data class Logprobs(
        val content: List<LogprobsContent>
    )

    data class LogprobsContent(
        val token: String,
        val logprob: Double,
        val bytes: List<Int>? = null,
        val topLogprobs: List<TopLogprobsContent>? = null
    )

    data class TopLogprobsContent(
        val token: String,
        val logprob: Double,
        val bytes: List<Int>? = null
    )

    @Serializable
    enum class FinishReason {
        Stop, Length, ContentFilter, ToolCalls, InsufficientSystemResource
    }

    @Serializable
    data class Usage(
        val promptTokens: Int? = null,
        val completionTokens: Int? = null,
        val totalTokens: Int? = null,
        val promptTokensDetails: PromptTokensDetails = PromptTokensDetails(),
        val completionTokensDetails: CompletionTokensDetails? = CompletionTokensDetails(),
        val promptCacheHitTokens: Int? = null,
        val promptCacheMissTokens: Int? = null
    )

    @Serializable
    data class PromptTokensDetails(
        val cachedTokens: Int? = null
    )

    @Serializable
    data class CompletionTokensDetails(
        val reasoningTokens: Int? = null
    )
}
```

接下来请欣赏——完整示例！

```kotlin
// 天才如你一定是不需要注释解释的吧(确信)!
fun main() {
    runBlocking {
        var m = Messages().system("你是一个巧克力蛋糕冰淇淋")
        val client = Client {
            apiKey = ApiKey.Key("Your Key")
        }

        while (true) {
            client.chatCompletionChunk {
                model = Model.Chat
                messages = m.let { print("\nUser:"); m = it.user(readln()); print("DeepSeek:"); m }
                stream = true
            }.collect {
                when (it) {
                    is ClientMessage.Success -> print(it.choices.first().message.content)
                    is ClientMessage.Error -> println()
                    is ClientMessage.Finish -> println()
                }
            }
        }
    }
}
```

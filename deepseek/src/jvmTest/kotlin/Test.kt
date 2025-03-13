import io.github.moyeranqianzhi.deepseek.SimpleRequest
import io.github.moyeranqianzhi.deepseek.api.ApiKey
import io.github.moyeranqianzhi.deepseek.api.Client
import io.github.moyeranqianzhi.deepseek.api.ClientMessage
import io.github.moyeranqianzhi.deepseek.api.Message
import io.github.moyeranqianzhi.deepseek.api.Messages
import io.github.moyeranqianzhi.deepseek.api.Model
import io.github.moyeranqianzhi.deepseek.api.toMessages
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Test

class Test {
    @Test
    fun `test json`() {
        val model = Model.Reasoner
        println(Json.encodeToString<Model>(model))
        println(Json.encodeToString<ApiKey>(ApiKey.Key("sk-key")))
        println(Json.encodeToString<Message>(Message.Assistant("Hello")))
        println(Json.decodeFromString<Message>("{\"role\":\"user\",\"content\":\"Hello\"}"))
        println(
            Messages(
                Message.User("Hello"),
                Message.Assistant("Hello")
            )
        )
        println(
            Json.encodeToString<Messages>(
                Messages(
                    Message.User("Hello"),
                    Message.Assistant("Hello")
                )
            )
        )
        val m = Messages {
            append(Message.User("Hello"))
            system("Hello")
            append(Message.Assistant("Hello"))
            user("Hello")
            add(Message.User("Hello"))
        }
        println(m.toList())
        println(
            listOf(
                Message.User("Hello"),
            ).toMessages()
        )
        println(
            Messages()
                .system("You are AI.")
                .user("Hello")
                .assistant("Hello")
                .user("Good")
                .assistant("Yes!")
        )
    }

    @Test
    fun `test client`() {
        val client = Client {
            apiKey = ApiKey.Key("Your Key")
        }
        runBlocking {
            client.chatCompletion {
                model = Model.Chat
                messages = Messages {
                    user("你好")
                }
            }.let { message ->
                when (message) {
                    is ClientMessage.Success -> {
                        println(message)
                    }
                    is ClientMessage.Error -> {
                        error("error")
                    }
                    ClientMessage.Finish -> {}
                }
            }
        }
    }

    @Test
    fun `test client chunk`() {
        val client = Client {
            apiKey = ApiKey.Key("Your key")
        }
        runBlocking {
            client.chatCompletionChunk {
                model = Model.Chat
                messages = Messages().user("你好")
            }.collect { message ->
                when (message) {
                    ClientMessage.Error -> {
                        error("error")
                    }

                    ClientMessage.Finish -> {
                        println("---------Finished-------")
                    }

                    is ClientMessage.Success -> {
                        println(message)
                    }
                }
            }
        }
    }
    @Test
    fun `test client chunk reasoning`() {
        val client = Client {
            apiKey = ApiKey.Key("Your Key")
        }
        runBlocking {
            client.chatCompletionChunk {
                model = Model.Reasoner
                messages = Messages {
                    user("你好")
                }
            }.collect { res ->
                when (res) {
                    ClientMessage.Error -> {
                        error("<|ERROR|>")
                    }
                    ClientMessage.Finish -> {
                        println("<|FINISHED|>")
                    }
                    is ClientMessage.Success -> {
                        println(res)
                    }
                }
            }
        }
    }

    @Test
    fun `test simple`() {
        Messages(
            Message.System("你是一个深索"),
            Message.User("你好，你是谁")
        )
//        val messages = Messages()
//        messages.append(Message.System("你是一个深索"))
//        messages.append(Message.User("你好，你是谁"))
//
//        var messages = Messages()
//        messages = messages.system("你是一个深索")
//        messages = messages.user("你好，你是谁")

        val messages = listOf(
            Message.System("你是一个深索"),
            Message.User("你好，你是谁")
        ).toMessages()

        runBlocking {
            SimpleRequest(
                apiKey = "sk-key",
                model = Model.Chat,
                messages = Messages {
                    system("你是一个测试机A520")
                    user("你好，你是谁")
                }
            ).chatCompletions().collect { print(it.second ?: "") }
        }
    }
}

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

const val RootClassName = "Test"

@OptIn(ExperimentalStdlibApi::class)
fun main(args: Array<String>) {
    println("Program arguments: ${args.joinToString()}")

    val jsonString = """
        {
            "name": "John",
            "age": 30,
            "title": {
                "romaji": "Cowboy Bebop",
                "english": "Cowboy Bebop",
                "native": "カウボーイビバップ"
            },
            "number_items": [1, 3, 4],
            "class_items": [
                {
                    "extraLarge": "bx5-NozHwXWdNLCz.jpg",
                    "large": "bx5-NozHwXWdNLCz.jpg"
                },
                {
                    "extraLarge": "vvvvvvvvvvvvvvv.jpg",
                    "large": "eeeeeeeeeeeeeeeee.jpg"
                }
            ]
        }
    """.trimIndent()

    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val jsonAdapter: JsonAdapter<Map<String, Any>> = moshi.adapter(typeOf<Map<String, Any>>().javaType)
    val jsonObject = jsonAdapter.fromJson(jsonString) ?: throw IllegalArgumentException("Invalid JSON")

//    println(jsonObject)
    generateDataKotlinClass(className = RootClassName, jsonObject)
}

fun generateDataKotlinClass(className: String, jsonMap: Map<String, Any>) {
    println("generateDataKotlinClass E $jsonMap")
    jsonMap.forEach { propertyName, propertyValue ->
        when (propertyValue) {
            is Map<*, *> -> {
                generateDataKotlinClass(className = propertyName, jsonMap = propertyValue as Map<String, Any>)
            }

            is List<*> -> {
                if (propertyValue.isNotEmpty()) {
                    (propertyValue.first() as? Map<String, Any>)?.let {
                        // TODO: change propertyName.
                        generateDataKotlinClass(className = propertyName,jsonMap = it)
                    }
                }
            }

            is String,
            is Number -> {
            }
        }
    }
    println("generateDataKotlinClass X")
}
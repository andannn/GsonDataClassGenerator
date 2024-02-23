import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.*
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

const val RootClassName = "Test"
val listClass = ClassName("kotlin.collections", "List")

@OptIn(ExperimentalStdlibApi::class)
fun main(args: Array<String>) {
    println("Program arguments: ${args.joinToString()}")

    val jsonString = """
        {
            "name": "John",
            "age": 30,
            "title_test": {
                "romaji": "Cowboy Bebop",
                "english": "Cowboy Bebop",
                "native": "カウボーイビバップ"
            },
            "number_items": [1, 3, 4],
            "image_items": [
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

    generateDataKotlinClass(className = RootClassName, jsonObject)
}

fun generateDataKotlinClass(className: String, jsonMap: Map<String, Any>) {
    println("generateDataKotlinClass E $jsonMap")
    if (jsonMap.isEmpty()) {
        println("Empty json map")
        return
    }

    val fileBuilder = FileSpec.builder(fileName = "$RootClassName.kt", packageName = "")

    val classBuilder = TypeSpec.classBuilder(className).addModifiers(KModifier.DATA)
    val constructorBuilder = FunSpec.constructorBuilder()
    jsonMap.forEach { (propertyName, propertyValue) ->
        val propertyNameCamel = propertyName.toLowerCamelCase()
        when (propertyValue) {
            is Map<*, *> -> {
                generateDataKotlinClass(
                    className = propertyName.toUpperCamelCase(),
                    jsonMap = propertyValue as Map<String, Any>
                )

                val mapClass = ClassName(packageName = "", simpleNames = listOf(propertyName.toUpperCamelCase()))
                (classBuilder to constructorBuilder).addPrimaryConstructor(propertyNameCamel, mapClass)
            }

            is List<*> -> {
                if (propertyValue.isNotEmpty()) {
                    (propertyValue.first() as? Map<String, Any>)?.let {
                        generateDataKotlinClass(className = propertyName.toUpperCamelCase(), jsonMap = it)

                        val listClass =
                            listClass.parameterizedBy(
                                ClassName(packageName = "", simpleNames = listOf(propertyName.toUpperCamelCase()))
                            )
                        (classBuilder to constructorBuilder).addPrimaryConstructor(
                            propertyName = propertyName.toLowerCamelCase(),
                            type = listClass
                        )
                        return@forEach
                    }

                    val listClass = listClass.parameterizedBy(propertyValue.first()!!::class.asTypeName())
                    (classBuilder to constructorBuilder).addPrimaryConstructor(propertyNameCamel, listClass)
                }
            }

            is String -> {
                (classBuilder to constructorBuilder).addPrimaryConstructor(
                    propertyNameCamel,
                    String::class.asTypeName()
                )
            }

            is Int -> {
                (classBuilder to constructorBuilder).addPrimaryConstructor(
                    propertyNameCamel,
                    Int::class.asTypeName()
                )
            }

            is Double -> {
                (classBuilder to constructorBuilder).addPrimaryConstructor(
                    propertyNameCamel,
                    Double::class.asTypeName()
                )
            }
        }
    }

    val output = fileBuilder.addType(
        classBuilder.primaryConstructor(
            constructorBuilder.build()
        ).build()
    ).build()

    println("$output")
}

fun Pair<TypeSpec.Builder, FunSpec.Builder>.addPrimaryConstructor(propertyName: String, type: TypeName) {
    val (classBuilder, constructorBuilder) = this
    classBuilder.addProperty(
        PropertySpec.builder(propertyName, type)
            .initializer(propertyName)
            .build()
    )
    constructorBuilder.addParameter(propertyName, type)
}

fun String.toLowerCamelCase() = let {
    it.split("_").foldIndexed("") { index, acc, element ->
        if (index == 0) {
            element
        } else {
            acc + element.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }
    }
}

fun String.toUpperCamelCase() = let {
    it.split("_").foldIndexed("") { index, acc, element ->
        acc + element.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }
}

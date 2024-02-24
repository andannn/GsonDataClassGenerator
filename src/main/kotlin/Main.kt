import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.xenomachina.argparser.ArgParser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

const val RootClassName = "Test"
val listClass = ClassName("kotlin.collections", "List")

class GsonClassGeneratorArgs(parser: ArgParser) {
    val source by parser.positional(
        "SOURCE",
        help = "source filename")

    val destination by parser.positional(
        "DEST",
        help = "destination filename")
}

@OptIn(ExperimentalStdlibApi::class)
fun main(args: Array<String>) {
    println("Program arguments: ${args.joinToString()}")

    val jsonString = """
{
  "data": {
    "Staff": {
      "id": 96764,
      "image": {
        "large": "https://s4.anilist.co/file/anilistcdn/staff/large/n96764-yJWGrhjanDJQ.png",
        "medium": "https://s4.anilist.co/file/anilistcdn/staff/medium/n96764-yJWGrhjanDJQ.png"
      }
    }
  }
}
    """.trimIndent()

    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val jsonAdapter: JsonAdapter<Map<String, Any>> = moshi.adapter(typeOf<Map<String, Any>>().javaType)
    val jsonObject = jsonAdapter.fromJson(jsonString) ?: throw IllegalArgumentException("Invalid JSON")

    val fileBuilder = FileSpec.builder(fileName = "$RootClassName.kt", packageName = "")
    fileBuilder.generateDataKotlinClass(className = RootClassName, jsonObject)

    val output = fileBuilder.build()

    println(output)
}

fun FileSpec.Builder.generateDataKotlinClass(className: String, jsonMap: Map<String, Any>) {
    if (jsonMap.isEmpty()) {
        println("Empty json map")
        return
    }

    val classBuilder = TypeSpec.classBuilder(className).addModifiers(KModifier.DATA)
    val constructorBuilder = FunSpec.constructorBuilder()
    jsonMap.forEach { (propertyName, propertyValue) ->
        when (propertyValue) {
            is Map<*, *> -> {
                generateDataKotlinClass(
                    className = propertyName.toUpperCamelCase(),
                    jsonMap = propertyValue as Map<String, Any>
                )

                val mapClass = ClassName(packageName = "", simpleNames = listOf(propertyName.toUpperCamelCase()))
                (classBuilder to constructorBuilder).addPrimaryConstructorAndAnnotaion(propertyName, mapClass)
            }

            is List<*> -> {
                if (propertyValue.isNotEmpty()) {
                    (propertyValue.first() as? Map<String, Any>)?.let {
                        generateDataKotlinClass(className = propertyName.toUpperCamelCase(), jsonMap = it)

                        val listClass =
                            listClass.parameterizedBy(
                                ClassName(packageName = "", simpleNames = listOf(propertyName.toUpperCamelCase()))
                            )
                        (classBuilder to constructorBuilder).addPrimaryConstructorAndAnnotaion(
                            propertyName = propertyName,
                            type = listClass
                        )
                        return@forEach
                    }

                    val listClass = listClass.parameterizedBy(propertyValue.first()!!::class.asTypeName())
                    (classBuilder to constructorBuilder).addPrimaryConstructorAndAnnotaion(propertyName, listClass)
                }
            }

            is String -> {
                (classBuilder to constructorBuilder).addPrimaryConstructorAndAnnotaion(
                    propertyName,
                    String::class.asTypeName()
                )
            }

            is Int -> {
                (classBuilder to constructorBuilder).addPrimaryConstructorAndAnnotaion(
                    propertyName,
                    Int::class.asTypeName()
                )
            }

            is Double -> {
                (classBuilder to constructorBuilder).addPrimaryConstructorAndAnnotaion(
                    propertyName,
                    Double::class.asTypeName()
                )
            }
        }
    }

    this.addType(
        classBuilder
            .primaryConstructor(
                constructorBuilder.build()
            )
            .addAnnotation(
                AnnotationSpec.builder(Serializable::class).build()
            )
            .build()
    )
}

fun Pair<TypeSpec.Builder, FunSpec.Builder>.addPrimaryConstructorAndAnnotaion(propertyName: String, type: TypeName) {
    val (classBuilder, constructorBuilder) = this
    val propertyNameLowerCamel = propertyName.toLowerCamelCase()
    classBuilder.addProperty(
        PropertySpec.builder(propertyNameLowerCamel, type)
            .initializer(propertyNameLowerCamel)
            .addAnnotation(
                AnnotationSpec.builder(SerialName::class)
                    .addMember("value = %S", propertyName)
                    .build()
            )
            .build()
    )
    constructorBuilder.addParameter(propertyNameLowerCamel, type)
}


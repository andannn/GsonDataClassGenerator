import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import kotlin.reflect.typeOf

val listClass = ClassName("kotlin.collections", "List")

class GsonClassGeneratorArgs(parser: ArgParser) {
    val source by parser.positional(
        "SOURCE",
        help = "source filename",
    )

    val destination by parser.storing(
        "-o", "--output",
        help = "output position"
    ).default(File(::main.javaClass.protectionDomain.codeSource.location.toURI()).parent.toString())

    val isDividedFile by parser.flagging(
        "--divide",
        help = "true if divide output file"
    )

    val packageName by parser.storing(
        "--package",
        help = "class package"
    ).default("com.xxx")
}

private var _arguments: GsonClassGeneratorArgs? = null
private val arguments get() = _arguments!!
private val destinationDictionary get() = File(arguments.destination)
private val divideFile get() = arguments.isDividedFile
private val packageName get() = arguments.packageName

fun main(args: Array<String>) {
    _arguments = ArgParser(args).parseInto(::GsonClassGeneratorArgs)

    val destinationDictionary = File(arguments.destination)
    check(destinationDictionary.exists() && destinationDictionary.isDirectory) {
        "Invalid output destination ${arguments.destination}"
    }

    val inputFile = File(arguments.source)
    check(inputFile.exists()) {
        "Invalid file path ${arguments.source}"
    }

    if (inputFile.isDirectory) {
        inputFile.listFiles { _, name ->
            name.contains(".json")
        }?.forEach {
            val jsonString = it.readText()
            val jsonFileName = it.absolutePath.substringAfterLast("/")

            parseJsonFile(jsonFileName = jsonFileName, jsonString = jsonString)
        }
    } else {
        val jsonString = inputFile.readText()
        val jsonFileName = inputFile.absolutePath.substringAfterLast("/")

        parseJsonFile(jsonFileName = jsonFileName, jsonString = jsonString)
    }

}

@OptIn(ExperimentalStdlibApi::class)
fun parseJsonFile(jsonFileName: String, jsonString: String) {
    println("------------------ parsing $jsonFileName E ------------------")
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val jsonAdapter: JsonAdapter<Map<String, Any>> = moshi.adapter(typeOf<Map<String, Any>>())
    val jsonObject = jsonAdapter.fromJson(jsonString) ?: throw IllegalArgumentException("Invalid JSON")

    val rootClassName = jsonFileName.substringBeforeLast(".").toUpperCamelCase()

    FileSpec.builder(fileName = rootClassName, packageName = packageName)
        .generateDataKotlinClass(
            className = rootClassName,
            jsonMap = jsonObject,
        ).build()
        .writeTo(destinationDictionary)

    println("------------------ parsing $jsonFileName X ------------------")
}

fun FileSpec.Builder.generateDataKotlinClass(
    className: String,
    jsonMap: Map<String, Any>,
): FileSpec.Builder {
    if (jsonMap.isEmpty()) {
        println("Empty json map")
        return this
    }

    val classBuilder = TypeSpec.classBuilder(className).addModifiers(KModifier.DATA)
    val constructorBuilder = FunSpec.constructorBuilder()
    jsonMap.forEach { (propertyName, propertyValue) ->
        when (propertyValue) {
            is Map<*, *> -> {
                if (divideFile) {
                    FileSpec.builder(fileName = propertyName.toUpperCamelCase(), packageName = packageName)
                        .generateDataKotlinClass(
                            className = propertyName.toUpperCamelCase(),
                            jsonMap = propertyValue as Map<String, Any>,
                        )
                        .build()
                        .writeTo(destinationDictionary)
                } else {
                    generateDataKotlinClass(
                        className = propertyName.toUpperCamelCase(),
                        jsonMap = propertyValue as Map<String, Any>,
                    )
                }

                val mapClass =
                    ClassName(packageName = packageName, simpleNames = listOf(propertyName.toUpperCamelCase()))
                (classBuilder to constructorBuilder).addPrimaryConstructorAndAnnotation(propertyName, mapClass)
            }

            is List<*> -> {
                if (propertyValue.isNotEmpty()) {
                    (propertyValue.first() as? Map<String, Any>)?.let {
                        val trimmedName = propertyName.substringBeforeLast("s")
                        if (divideFile) {
                            FileSpec.builder(fileName = trimmedName.toUpperCamelCase(), packageName = packageName)
                                .generateDataKotlinClass(
                                    className = trimmedName.toUpperCamelCase(),
                                    jsonMap = it,
                                )
                                .build()
                                .writeTo(destinationDictionary)
                        } else {
                            generateDataKotlinClass(
                                className = trimmedName.toUpperCamelCase(),
                                jsonMap = it,
                            )
                        }

                        val listClass =
                            listClass.parameterizedBy(
                                ClassName(
                                    packageName = packageName,
                                    simpleNames = listOf(trimmedName.toUpperCamelCase())
                                )
                            )
                        (classBuilder to constructorBuilder).addPrimaryConstructorAndAnnotation(
                            propertyName = propertyName,
                            type = listClass
                        )
                        return@forEach
                    }

                    val listClass = listClass.parameterizedBy(propertyValue.first()!!::class.asTypeName())
                    (classBuilder to constructorBuilder).addPrimaryConstructorAndAnnotation(propertyName, listClass)
                }
            }

            is String -> {
                (classBuilder to constructorBuilder).addPrimaryConstructorAndAnnotation(
                    propertyName,
                    String::class.asTypeName()
                )
            }

            is Int -> {
                (classBuilder to constructorBuilder).addPrimaryConstructorAndAnnotation(
                    propertyName,
                    Int::class.asTypeName()
                )
            }

            is Double -> {
                (classBuilder to constructorBuilder).addPrimaryConstructorAndAnnotation(
                    propertyName,
                    Double::class.asTypeName()
                )
            }
        }
    }

    return this.addType(
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

fun Pair<TypeSpec.Builder, FunSpec.Builder>.addPrimaryConstructorAndAnnotation(propertyName: String, type: TypeName) {
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


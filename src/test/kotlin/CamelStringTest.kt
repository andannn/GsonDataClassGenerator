import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CamelStringTest {

    @Test
    fun lower_camel_string_test() {
        assertEquals("lowerCamelStringTest", "lower_camel_string_test".toLowerCamelCase())
        assertEquals("lowerCamelStringTest", "lowerCamelStringTest".toLowerCamelCase())
    }

    @Test
    fun upper_camel_string_test() {
        assertEquals("LowerCamelStringTest", "lower_camel_string_test".toUpperCamelCase())
        assertEquals("LowerCamelStringTest", "lowerCamelStringTest".toUpperCamelCase())
    }
}
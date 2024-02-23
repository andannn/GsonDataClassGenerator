package com.andannn.gsonclassgen.util

import java.util.*

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

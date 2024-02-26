### Simple tool to generate Kotlin data class form json string.

```
java -jar data-class-generator-0.2.jar [-h] SOURCE [-o OUTPUT] [-d] [-p PACKAGE]
```

**optional arguments**:

| Arg                  |                                          Description |
|:---------------------|-----------------------------------------------------:|
| -h, --help           |                      show this help message and exit |
| -o/--output OUTPUT   |                                  output destination. |
| -d/ --divide         | whether to generate the kt file for each json block. |
| -p/--package PACKAGE |                              package name of kt file |

**positional arguments**:

SOURCE source filename or dictionary.

| Arg    |                   Description |
|:-------|------------------------------:|
| SOURCE | source filename or dictionary |
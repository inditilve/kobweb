package com.varabyte.kobweb.gradle.application.templates

import com.varabyte.kobweb.gradle.application.BuildTarget

fun createHtmlFile(title: String, headElements: List<String>, src: String, buildTarget: BuildTarget): String = """
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="description" content="Generaged by Kobweb" />
        <title>$title</title>
        <link rel="icon" href="/favicon.ico" />
        ${headElements.joinToString("\n        ")}
    </head>
    <body>
        <div id="root"></div>
${
    if (buildTarget == BuildTarget.DEBUG) {
        """
        |        <div id="status"><span id="warning">❌</span><span id="spinner">🕸️</span> <span id="text"></span>
        |            <style>
        |                @keyframes kobweb-spin {
        |                    from { transform: rotate(0deg); }
        |                    to { transform: rotate(359deg); }
        |                }
        |                body > #status {
        |                    position: fixed;
        |                    font-size: 24px;
        |                    background: whitesmoke;
        |                    top: 20px;
        |                    left: 50%;
        |                    transform: translateX(-50%);
        |                    padding: 10px;
        |                    border: 1px solid;
        |                    border-radius: 10px;
        |                    visibility: hidden;
        |                    opacity: 0;
        |                }
        |                body > #status > .hidden {
        |                   display: none;
        |                }
        |                body > #status > .visible {
        |                    display: inline-block;
        |                }
        |
        |                body > #status.fade-in {
        |                    visibility: visible;
        |                    opacity: 1;
        |                    transition: opacity 1s;
        |                }
        |                body > #status.fade-out {
        |                   visibility: hidden;
        |                   opacity: 0;
        |                   transition: visibility 0s 1s, opacity 1s;
        |                }
        |                body > #status > #spinner {
        |                    animation: kobweb-spin 1.5s linear infinite;
        |                }
        |            </style>
        |        </div>
    """.trimMargin("|")
    } else {
        ""
    }
}

        <script src="$src"></script>
    </body>
    </html>
""".trimIndent()
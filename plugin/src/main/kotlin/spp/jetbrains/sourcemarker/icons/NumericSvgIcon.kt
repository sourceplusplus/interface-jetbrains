/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.sourcemarker.icons

/**
 * todo: description.
 *
 * @since 0.2.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MagicNumber", "MaxLineLength")
data class NumericSvgIcon(val number: Int, val color: String) {

    private val numerals = mapOf(
        0 to "<path class=\"st2\" d=\"M3.6,9.3C3.1,8.9,2.9,8.2,2.9,7.5V3.6c0-0.8,0.2-1.4,0.7-1.9C4.1,1.3,4.7,1,5.5,1s1.5,0.3,2,0.7\n" +
                "\t\t\tC8,2.2,8.2,2.8,8.2,3.6v3.8c0,0.8-0.2,1.4-0.7,1.9C7,9.8,6.3,10,5.5,10S4.1,9.8,3.6,9.3z M6.7,8.7C7,8.3,7.1,7.9,7.1,7.5V3.6\n" +
                "\t\t\tc0-0.5-0.1-0.9-0.4-1.2C6.4,2.1,6,2,5.5,2S4.7,2.1,4.4,2.4C4.1,2.7,3.9,3.1,3.9,3.6v3.8c0,0.5,0.1,0.9,0.4,1.2\n" +
                "\t\t\tC4.7,9,5.1,9.1,5.5,9.1S6.4,9,6.7,8.7z M5.1,6.1C5,6,4.9,5.8,4.9,5.6V5.4C4.9,5.2,5,5,5.1,4.9s0.3-0.2,0.4-0.2\n" +
                "\t\t\tc0.2,0,0.3,0.1,0.4,0.2s0.2,0.3,0.2,0.5v0.2c0,0.2,0,0.4-0.1,0.5S5.7,6.3,5.5,6.3C5.4,6.3,5.2,6.2,5.1,6.1z\"/>",
        1 to "<path d=\"M8.2,9.9H2.8v-1h2.4V2.1L2.8,3.8V2.6l2-1.5h1.5v7.8h1.9V9.9z\"/>",
        2 to "<path d=\"M3.1,10V8.9L5.7,6c0.5-0.5,0.8-0.9,1-1.3S7,4,7,3.6c0-0.5-0.1-0.9-0.4-1.2C6.4,2.2,6,2.1,5.5,2.1S4.6,2.2,4.3,2.5\n" +
                "\t\t\tC4,2.8,3.9,3.2,3.9,3.7H2.8c0-0.8,0.3-1.4,0.8-1.9s1.1-0.7,1.9-0.7s1.4,0.2,1.9,0.7s0.7,1.1,0.7,1.8c0,0.5-0.1,1-0.4,1.5\n" +
                "\t\t\tC7.5,5.6,7,6.1,6.4,6.8L4.3,9h4v1H3.1z\"/>",
        3 to "<path d=\"M5.7,4.3c0.8,0,1.4,0.3,1.8,0.7c0.4,0.5,0.7,1,0.7,1.8v0.6c0,0.5-0.1,0.9-0.3,1.3c-0.3,0.5-0.6,0.8-1,1S6,10,5.5,10\n" +
                "\t\t\ts-1-0.1-1.4-0.3S3.4,9.2,3.2,8.8C3,8.4,2.9,8,2.9,7.5H4C4,8,4.1,8.4,4.4,8.6S5.1,9,5.5,9C6,9,6.4,8.9,6.7,8.6\n" +
                "\t\t\tC7,8.3,7.1,7.9,7.1,7.5V6.8C7.1,6.3,7,6,6.7,5.7S6,5.3,5.5,5.3H4.6V4.2l2.2-2.1H3.3v-1h4.6v1.1L5.7,4.3z\"/>",
        4 to "<path d=\"M8.1,9.9H7V8H3V6.3l3.5-5.1h1.2L4.1,6.5V7H7V4.9h1.1V9.9z\"/>",
        5 to "<path d=\"M7.5,4.8c0.4,0.4,0.6,1.1,0.6,1.8v0.9c0,0.8-0.2,1.4-0.7,1.8C7,9.8,6.3,10,5.5,10c-0.7,0-1.3-0.2-1.8-0.6\n" +
                "\t\t\tC3.3,9.1,3,8.6,3,7.9h1c0,0.3,0.2,0.6,0.4,0.8S5.1,9,5.5,9C6,9,6.4,8.9,6.7,8.6C6.9,8.4,7,8,7,7.5V6.6c0-0.5-0.1-0.9-0.4-1.2\n" +
                "\t\t\tC6.4,5.2,6,5,5.5,5C5.2,5,5,5.1,4.7,5.3C4.5,5.5,4.3,5.7,4.2,5.9H3.1l0.1-4.8h4.6v1H4.2V5l0,0c0.1-0.3,0.3-0.5,0.6-0.7\n" +
                "\t\t\tc0.3-0.2,0.7-0.2,1.1-0.2C6.6,4.1,7.1,4.3,7.5,4.8z\"/>",
        6 to "<path d=\"M4.1,9.6c-0.4-0.2-0.8-0.6-1-1C2.9,8.2,2.8,7.7,2.8,7.2c0-0.9,0.2-1.7,0.7-2.5l2.2-3.5h1.2l-2.5,4l0,0\n" +
                "\t\t\tC4.6,5,4.8,4.8,5,4.7c0.3-0.2,0.6-0.3,1-0.3c0.7,0,1.3,0.2,1.7,0.7s0.6,1.2,0.6,2c0,0.6-0.1,1.1-0.3,1.5C7.8,9,7.4,9.4,7,9.6\n" +
                "\t\t\tC6.6,9.9,6.1,10,5.6,10C5,10,4.5,9.9,4.1,9.6z M6.8,8.5c0.3-0.3,0.5-0.8,0.5-1.4s-0.2-1-0.5-1.3C6.5,5.5,6.1,5.3,5.5,5.3\n" +
                "\t\t\tC5,5.3,4.6,5.5,4.3,5.8C4,6.1,3.8,6.6,3.8,7.2s0.2,1,0.5,1.4C4.6,8.9,5,9,5.5,9C6.1,9,6.5,8.9,6.8,8.5z\"/>",
        7 to "<path d=\"M5.2,9.9H4.1l3.1-7.8H3.9v1.3H2.8V1.2h5.5v1.1L5.2,9.9z\"/>",
        8 to "<path d=\"M7.9,6.1C8.2,6.5,8.3,7,8.3,7.5c0,0.8-0.2,1.4-0.7,1.8C7.1,9.8,6.4,10,5.5,10c-0.8,0-1.5-0.2-2-0.7C3,8.9,2.8,8.3,2.8,7.6\n" +
                "\t\t\tc0-0.6,0.1-1.1,0.4-1.5s0.7-0.7,1.1-0.8C3.9,5.2,3.6,5,3.3,4.6C3.1,4.2,3,3.8,3,3.3c0-0.4,0.1-0.8,0.3-1.2\n" +
                "\t\t\tc0.2-0.3,0.5-0.6,0.9-0.8S5,1,5.5,1s1,0.1,1.4,0.3s0.7,0.5,0.9,0.8C8,2.5,8.1,2.9,8.1,3.3c0,0.5-0.1,0.9-0.4,1.3\n" +
                "\t\t\tC7.5,5,7.2,5.2,6.8,5.3C7.2,5.4,7.6,5.7,7.9,6.1z M6.8,8.7C7,8.4,7.2,8,7.2,7.4c0-0.5-0.1-1-0.4-1.2S6.1,5.7,5.5,5.7\n" +
                "\t\t\tS4.6,5.9,4.3,6.2C4,6.5,3.9,6.9,3.9,7.4s0.1,1,0.4,1.3C4.6,9,5,9.1,5.5,9.1S6.5,9,6.8,8.7z M4.5,2.3C4.2,2.6,4.1,2.9,4.1,3.4\n" +
                "\t\t\ts0.1,0.9,0.4,1.1C4.7,4.8,5.1,5,5.5,5C6,5,6.4,4.8,6.6,4.5C6.9,4.3,7,3.9,7,3.4S6.9,2.6,6.6,2.3C6.4,2.1,6,1.9,5.5,1.9\n" +
                "\t\t\tC5.1,1.9,4.7,2.1,4.5,2.3z\"/>",
        9 to "<path d=\"M4.2,10l2.5-4l0,0C6.5,6.2,6.3,6.4,6,6.5S5.4,6.7,5.1,6.7C4.4,6.7,3.8,6.5,3.4,6C3,5.4,2.8,4.8,2.8,3.9\n" +
                "\t\t\tc0-0.6,0.1-1.1,0.3-1.5s0.6-0.8,1-1S5,1.1,5.5,1.1c0.6,0,1,0.1,1.5,0.4c0.4,0.2,0.8,0.6,1,1S8.3,3.4,8.3,4c0,0.9-0.2,1.7-0.7,2.5\n" +
                "\t\t\tL5.4,10H4.2z M6.8,5.3C7.1,5,7.3,4.5,7.3,3.9s-0.2-1-0.5-1.4C6.5,2.2,6.1,2.1,5.5,2.1C5,2.1,4.6,2.2,4.3,2.6\n" +
                "\t\t\tC4,2.9,3.8,3.4,3.8,3.9s0.2,1,0.5,1.4S5,5.8,5.5,5.8C6.1,5.8,6.5,5.6,6.8,5.3z\"/>"
    )

    override fun toString(): String {
        val h =
            "<path d=\"M7.5,3.8c0.4,0.4,0.6,1,0.6,1.7v4.4H7V5.6c0-0.5-0.1-0.8-0.4-1.1C6.3,4.3,6,4.1,5.6,4.1c-0.5,0-0.8,0.1-1.1,0.4\n" +
                    "\t\t\tC4.2,4.8,4.1,5.2,4.1,5.7v4.2H3V1.2h1.1v3.4h0c0.1-0.4,0.2-0.8,0.5-1C5,3.3,5.4,3.2,5.9,3.2C6.6,3.2,7.1,3.4,7.5,3.8z\"/>"
        val k = "<path d=\"M3.9,9.9H2.8V1.2h1.1V6h1.2L7,3.3h1.2L6,6.5L8.3,10H7L5.1,7H3.9V9.9z\"/>"
        val m =
            "<path d=\"M8,3.6c0.2,0.3,0.3,0.7,0.3,1.2v5h-1v-5c0-0.3-0.1-0.5-0.2-0.6C7.1,4.1,6.9,4,6.7,4C6.5,4,6.3,4.1,6.2,4.2\n" +
                    "\t\t\tC6,4.4,6,4.6,6,4.9v5H5.1v-5c0-0.3-0.1-0.5-0.2-0.6C4.8,4.1,4.6,4,4.4,4C4.2,4,4,4.1,3.9,4.2C3.8,4.4,3.7,4.6,3.7,4.9v5h-1V3.3\n" +
                    "\t\t\th0.9v0.8h0.1c0-0.3,0.2-0.5,0.4-0.7c0.2-0.2,0.4-0.2,0.7-0.2c0.3,0,0.5,0.1,0.7,0.2c0.2,0.2,0.3,0.4,0.4,0.7h0\n" +
                    "\t\t\tC6,3.8,6.1,3.6,6.3,3.4C6.5,3.3,6.8,3.2,7,3.2C7.5,3.2,7.8,3.3,8,3.6z\"/>"
        val b =
            "<path d=\"M7.5,3.8c0.4,0.4,0.6,1,0.6,1.8v1.9c0,0.8-0.2,1.3-0.6,1.8C7.1,9.8,6.6,10,5.9,10C5.4,10,5,9.9,4.7,9.6\n" +
                    "\t\t\tC4.4,9.4,4.2,9,4.1,8.6l0,0v1.3H3V1.1h1.1V3v1.5l0,0c0.1-0.4,0.3-0.8,0.6-1C5,3.3,5.4,3.1,5.9,3.1C6.6,3.1,7.1,3.4,7.5,3.8z\n" +
                    "\t\t\t M7,5.7c0-0.5-0.1-0.9-0.4-1.2S6,4.1,5.5,4.1c-0.4,0-0.8,0.1-1.1,0.4C4.2,4.8,4.1,5.2,4.1,5.7v1.8c0,0.5,0.1,0.9,0.4,1.2\n" +
                    "\t\t\tC4.7,8.9,5.1,9,5.5,9C6,9,6.3,8.9,6.6,8.6S7,8,7,7.5V5.7z\"/>"
        val t = """
            <svg xmlns="http://www.w3.org/2000/svg" width="20px" height="20px" viewBox="0 0 448 512">
    <g>
        <style type="text/css">
            .blue{fill:$color;}
        </style>
        <path class="blue"
              d="M400 32H48C21.5 32 0 53.5 0 80v352c0 26.5 21.5 48 48 48h352c26.5 0 48-21.5 48-48V80c0-26.5-21.5-48-48-48z"/>
        <svg x="-10px" y="0px" viewBox="0 0 22.2 11.1">
            <style type="text/css">
                .st0{fill:#F2F2F2;}
                .st1{display:none;}
                .st2{display:inline;}
            </style>
            <g id="_x33_" class="st0">
                <g class="st2">
        """.trimIndent()

        val t2 = """
                </g>
            </g>
        </svg>

        <svg x="110px" y="0px" viewBox="0 0 22.2 11.1">
            <style type="text/css">
                .st0{fill:#F2F2F2;}
                .st1{display:none;}
                .st2{display:inline;}
            </style>
            <g id="_x33_" class="st0">
                <g class="st2">
        """.trimIndent()

        val t3 = """
                            </g>
                        </g>
                    </svg>

                    <svg x="235px" y="0px" viewBox="0 0 22.2 11.1">
                        <style type="text/css">
                            .st0{fill:#F2F2F2;}
                            .st1{display:none;}
                            .st2{display:inline;}
                        </style>
                        <g id="_x34_" class="st0">
                            <g class="st2">
        """.trimIndent()

        val t4 = """
                            </g>
            </g>
        </svg>
    </g>
</svg>
        """.trimIndent()

        when {
            number >= 1_000_000_000 -> {
                return t + numerals[number.toString()[0].toString().toInt()] + t2 + b + t3 + t4
            }
            number >= 100_000_000 -> {
                return t + numerals[number.toString()[0].toString().toInt()] +
                        t2 + h + t3 + m + t4
            }
            number >= 10_000_000 -> {
                return t + numerals[number.toString()[0].toString().toInt()] +
                        t2 + numerals[number.toString()[1].toString().toInt()] +
                        t3 + m + t4
            }
            number >= 1_000_000 -> {
                return t + numerals[number.toString()[0].toString().toInt()] + t2 + m + t3 + t4
            }
            number >= 100_000 -> {
                return t + numerals[number.toString()[0].toString().toInt()] +
                        t2 + h + t3 + k + t4
            }
            number >= 10_000 -> {
                return t + numerals[number.toString()[0].toString().toInt()] +
                        t2 + numerals[number.toString()[1].toString().toInt()] +
                        t3 + k + t4
            }
            number >= 1_000 -> {
                return t + numerals[number.toString()[0].toString().toInt()] +
                        t2 + numerals[number.toString()[1].toString().toInt()] +
                        t3 + h + t4
            }
            number >= 100 -> {
                return t + numerals[number.toString()[0].toString().toInt()] +
                        t2 + numerals[number.toString()[1].toString().toInt()] +
                        t3 + numerals[number.toString()[2].toString().toInt()] + t4
            }
            number >= 10 -> {
                return t + numerals[number.toString()[0].toString().toInt()] +
                        t2 + numerals[number.toString()[1].toString().toInt()] + t3 + t4
            }
            else -> {
                return t + numerals[number.toString()[0].toString().toInt()] + t2 + t3 + t4
            }
        }
    }
}

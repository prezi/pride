package com.prezi.pride

import spock.lang.Specification
import spock.lang.Unroll

class PridePluginTest extends Specification {
    @Unroll
    def "CompareVersions #a <> #b"() {
        expect:
        PridePlugin.compareVersions(a, b) == result

        where:
        a       | b       | result
        "1.0"   | "1.0"   | 0
        "2.0"   | "1.0"   | 1
        "1.0"   | "2.0"   | -1
        "1.1"   | "1.0"   | 1
        "1.0"   | "1.1"   | -1
        "1.0"   | "1.0.1" | -1
        "1.0.1" | "1.0"   | 1
    }
}

package com.grailsrocks.functionaltest.client

import groovyx.net.http.ParserRegistry

class EvilWizardsKilledByFireIncantationParserRegistry extends ParserRegistry {
    Closure getAt(Object type) {
        return DEFAULT_PARSER
    }
}

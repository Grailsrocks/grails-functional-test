package com.grailsrocks.functionaltest.client

import groovyx.net.http.ParserRegistry

/*
 * This exists purely to circumvent HttpBuilder's horrible parser registry that does nothing
 * but cause exceptions and confusion as you have no idea what you will get back from it
 */
class EvilWizardsKilledByFireIncantationParserRegistry extends ParserRegistry {
    Closure getAt(Object type) {
        return DEFAULT_PARSER
    }
}

package com.grailsrocks.functionaltest.client

interface ClientAdapter {
    void requestSent(Client client)

    void contentChanged(ContentChangedEvent event)

    void printlnToTestReport(String s)
    void printlnToConsole(String s)
}
/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The original code of this plugin was developed by Historic Futures Ltd.
 * (www.historicfutures.com) and open sourced.
 */
package com.grailsrocks.functionaltest.client.htmlunit

import com.gargoylesoftware.htmlunit.html.HtmlButton
import com.gargoylesoftware.htmlunit.html.HtmlButtonInput
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput
import com.gargoylesoftware.htmlunit.html.HtmlFileInput
import com.gargoylesoftware.htmlunit.html.HtmlImageInput
import com.gargoylesoftware.htmlunit.html.HtmlInput
import com.gargoylesoftware.htmlunit.html.HtmlRadioButtonInput
import com.gargoylesoftware.htmlunit.html.HtmlResetInput
import com.gargoylesoftware.htmlunit.html.HtmlSelect
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput
import com.gargoylesoftware.htmlunit.html.HtmlTextArea

class FormWrapper {
    def form
    def fields
    def radioButtons
    def selects
    def fieldPrefixStack = new ArrayList()
    def test

    static BUTTON_CLASSES = [
        HtmlSubmitInput,
        HtmlResetInput,
        HtmlButton,
        HtmlButtonInput,
        HtmlImageInput
    ]

    FormWrapper(test, form) {
        this.@test = test
        this.@form = form
        this.@fields = new FieldsWrapper(form)
        this.@selects = new SelectsWrapper(form)
        this.@radioButtons = new RadioButtonsWrapper(form)
    }

    // Get any kind of field's value, if not explicitly scoped
    def getProperty(String name) {
        switch (name) {
            case 'fields': return fields
            case 'selects': return selects
            case 'radioButtons': return radioButtons
            default:
                def f = findField(name)
                if (f) {
                    return getFieldValue(f)
                } else {
                    throw new IllegalArgumentException("Unable to get field value, there is no element with name or id [$name]")
                }
        }
    }

    // Set the value of any kind of field,
    void setProperty(String name, value) {
        switch (name) {
            case 'fields':
            case 'selects':
            case 'radioButtons':
                throw new RuntimeException("Property $name is read-only")
            default:
                def f = findField(name)
                if (f) {
                    setFieldValue(f, value)
                } else {
                    throw new IllegalArgumentException("Unable to set field value, there is no element with name or id [$name]")
                }
                break
        }
    }

    /**
     * Find form element by id, name or value
     */
    private handleFormClick(idOrNameOrValueOrText) {
        def f
        f = test.byId(idOrNameOrValueOrText)
        if (!f) {
            def candidates = form.htmlElementDescendants
            f = candidates.find { n ->
                if (n.tagName == 'input') {
                    if (n.getAttribute('type') == 'submit') {
                        return n.getAttribute('value') == idOrNameOrValueOrText
                    } else {
                        return false
                    }
                } else if (n.tagName in ['button', 'a']) {
                    return n.textContent.trim() == idOrNameOrValueOrText.trim()
                }
            }
        }
        if (!f) {
            throw new IllegalArgumentException("Unable to 'click' element named '$idOrNameOrValueOrText', no elements with that name or text found, try clicking by value or id")
        }
        println "Clicked [$idOrNameOrValueOrText] which resolved to a [${f.class}]"
        f.click()
        // Events are triggered

        // Now let's see if it was a redirect
        this.@test.handleRedirects()
    }

    void click(String idOrNameOrValue) {
        handleFormClick(idOrNameOrValue)
    }

    // Either Get any kind of field if there is a single arg set value/selected, or set all properties if param is a map
    // or if param is a closure, prefix all assignments/access to properties separated by "." in the name for each level of nesting
    def invokeMethod(String name, Object args) {
        if ((args?.size() == 1) && (args[0] instanceof Closure)) {
            fieldPrefixStack << name
            // from here on we will be re-entrant...
            args[0].delegate = this
            args[0].resolveStrategy = Closure.DELEGATE_FIRST
            args[0].call()
            // no longer re-entrant (phew)
            fieldPrefixStack.pop()
            return null
        }

        def fqn = ''
        if (fieldPrefixStack) fqn = fieldPrefixStack.inject('') { output, value -> output += value + '.'; return output }
        fqn += name

        def f = findField(fqn)
        if (f) {
            if (args?.size() == 1) {
                if (args[0] instanceof Map) {
                    args[0].each { k, v ->
                        f[k] = v
                    }
                } else {
                    setFieldValue(f, args[0])
                }
            } else throw new MissingMethodException(name, getClass(), args)
            return f
        } else throw new IllegalArgumentException("No field could be found with name '$name'")
    }

    private setFieldValue(f, v) {
        // Note this switch is polymorphic, order of cases is IMPORTANT
        switch (f.class) {
            case BUTTON_CLASSES:
            case HtmlFileInput:
                throw new RuntimeException("You cannot set elements of type [${f.class}], call methods or set properties on them instead")
            case HtmlSelect:
                f.select(v)
                break
            case HtmlRadioButtonInput:
                f.checked = v.toString()
                break
            case HtmlCheckBoxInput:
                f.checked = Boolean.valueOf(v)
                break
            case HtmlTextArea:
            case HtmlInput:
                f.value = v
                break
            case RadioGroupWrapper:
                return f.checked = v
            default:
                throw new RuntimeException("You cannot set elements of type [${f.class}], call methods or set properties on them instead")
        }
    }

    private getFieldValue(f) {
        // Note this switch is polymorphic, order of cases is IMPORTANT
        switch (f.class) {
            case BUTTON_CLASSES:
            case HtmlFileInput:
                return f // return the field object itself, not its value
            case HtmlSelect:
                return f.selected
            case HtmlRadioButtonInput:
                return f.checked
            case HtmlCheckBoxInput:
                return f.checked
            case HtmlTextArea:
            case HtmlInput:
                return f.value
            case RadioGroupWrapper:
                return f.checked
            default:
                throw new RuntimeException("Don't know how to get a value from form element of type [${f.class}]")
        }
    }

    private findField(name) {
        def f = this.@radioButtons[name]
        if (f == null) {
            f = this.@selects[name]
        }
        // Check standard inputs last, to avoid polymorphism problems
        if (f == null) {
            f = this.@fields[name]
        }
        // Try for any input button by value
        if (f == null) {
            f = form.getInputsByValue(name)?.find { FormWrapper.BUTTON_CLASSES.contains(it.class) }
        }
        if (!f)
            throw new IllegalArgumentException(
                "No field with name [$name] could be found in form [name:${form.nameAttribute} id:${form.id}]")

        return f
    }
}

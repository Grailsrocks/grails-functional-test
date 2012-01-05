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

class RadioGroupWrapper {
    List radioButtons
    String fieldName
    
    RadioGroupWrapper(fieldName, radioButtons) {
        this.radioButtons = radioButtons
        this.fieldName = fieldName
    }
    
    void setChecked(String value) {
        System.out.println("Checking radio button [$value] on field [name:${fieldName}]") 
        def radio = radioButtons.find { it.value == value }
        if (radio) {
            radio.checked = true
        } else {
            throw new IllegalArgumentException("No radio button with name '$fieldName' for value '$value' found")
        }
    }

    String getChecked() {
        def radio = radioButtons.find { it.checked }
        return radio ? radio.value : null;
    }
}

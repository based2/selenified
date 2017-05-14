/*
 * Copyright 2017 Coveros, Inc.
 * 
 * This file is part of Selenified.
 * 
 * Selenified is licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy 
 * of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on 
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
 * KIND, either express or implied. See the License for the 
 * specific language governing permissions and limitations 
 * under the License.
 */

package tools.output;

public class Selenium {

	/**
	 * Select a Locator for the element we are interacting with Available
	 * options are: xpath, id, name, classname, paritallinktext, linktext,
	 * tagname
	 */
	public enum Locators {
		xpath, id, name, classname, paritallinktext, linktext, tagname
	}

	/**
	 * Select a browser to run Available options are: HtmlUnit (only locally -
	 * not on grid), Firefox, Chrome, InternetExplorer, Android, Ipad (only
	 * locally - not on grid), Iphone (only locally, not on grid, Opera, Safari
	 */
	public enum Browsers {
		None, HtmlUnit, Firefox, Marionette, Chrome, InternetExplorer, Edge, Android, Ipad, Iphone, Opera, Safari, PhantomJS
	}
}
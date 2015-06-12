/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Arbalo AG
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.opentdc.addressbooks.file;

import java.util.ArrayList;
import java.util.List;

import org.opentdc.addressbooks.AddressbookModel;

public class ABaddressbook {
	private AddressbookModel model;
	private ArrayList<ABcontact> contacts;
	private ArrayList<ABorg> orgs;
	
	public ABaddressbook() {
		contacts = new ArrayList<ABcontact>();
		setOrgs(new ArrayList<ABorg>());
	}
	
	public ABaddressbook(AddressbookModel addressbookModel) {
		contacts = new ArrayList<ABcontact>();
		setOrgs(new ArrayList<ABorg>());
		this.model = addressbookModel;
	}
	
	public AddressbookModel getModel() {
		return model;
	}
	
	public void setModel(AddressbookModel addressbookModel) {
		this.model = addressbookModel;
	}
	
	public List<ABcontact> getContacts() {
		return contacts;
	}
	
	public void setContacts(ArrayList<ABcontact> contacts) {
		this.contacts = contacts;
	}
	
	public void addContact(ABcontact contact) {
		this.contacts.add(contact);
	}
	
	public boolean removeContact(ABcontact contact) {
		return this.contacts.remove(contact);
	}

	/**
	 * @return the orgs
	 */
	public ArrayList<ABorg> getOrgs() {
		return orgs;
	}

	/**
	 * @param orgs the orgs to set
	 */
	public void setOrgs(ArrayList<ABorg> orgs) {
		this.orgs = orgs;
	}
	
	public void addOrg(ABorg org) {
		this.orgs.add(org);
	}
	
	public boolean removeOrg(ABorg org) {
		return this.orgs.remove(org);
	}
}

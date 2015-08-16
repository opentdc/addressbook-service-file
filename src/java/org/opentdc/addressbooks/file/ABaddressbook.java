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
	private ArrayList<String> contactIds;
	private ArrayList<String> orgIds;
	
	public ABaddressbook() {
		contactIds = new ArrayList<String>();
		orgIds = new ArrayList<String>();
	}
	
	public ABaddressbook(AddressbookModel addressbookModel) {
		contactIds = new ArrayList<String>();
		orgIds = new ArrayList<String>();
		this.model = addressbookModel;
	}
	
	public AddressbookModel getModel() {
		return model;
	}
	
	public void setModel(AddressbookModel addressbookModel) {
		this.model = addressbookModel;
	}
	
	public List<String> getContacts() {
		return contactIds;
	}
	
	public boolean containsContact(String cid) {
		for (String _id : contactIds) {
			if (_id.equalsIgnoreCase(cid)) {
				return true;
			}
		}
		return false;
	}
		
	public void addContact(String cid) {
		this.contactIds.add(cid);
	}
	
	public boolean removeContact(String cid) {
		return this.contactIds.remove(cid);
	}

	/**
	 * @return the orgs
	 */
	public ArrayList<String> getOrgs() {
		return orgIds;
	}

	public boolean containsOrg(String oid) {
		for (String _id : orgIds) {
			if (_id.equalsIgnoreCase(oid)) {
				return true;
			}
		}
		return false;
	}
		
	/**
	 * @param orgs the orgs to set
	 */
	public void setOrgs(ArrayList<String> orgs) {
		this.orgIds = orgs;
	}
	
	public void addOrg(String oid) {
		this.orgIds.add(oid);
	}
	
	public boolean removeOrg(String oid) {
		return this.orgIds.remove(oid);
	}
}

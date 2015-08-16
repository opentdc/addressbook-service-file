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
import java.util.Date;

import org.opentdc.addressbooks.AddressModel;
import org.opentdc.addressbooks.OrgModel;

public class ABorg {
	private OrgModel model;
	private ArrayList<AddressModel> addresses;
	private ArrayList<String> memberships;  // lists ids of addressbooks where this org is a member of
	
	public ABorg() {
		addresses = new ArrayList<AddressModel>();
		memberships = new ArrayList<String>();
	}

	public OrgModel getModel() {
		return model;
	}

	public void setModel(OrgModel orgModel) {
		this.model = orgModel;
	}

	public ArrayList<AddressModel> getAddresses() {
		return addresses;
	}
	
	public void setAddresses(ArrayList<AddressModel> addresses) {
		this.addresses = addresses;
	}
	
	public void addAddress(AddressModel address) {
		this.addresses.add(address);
	}
	
	public void replaceAddress(AddressModel address) {
		int _index = 0;
		for (_index = 0; _index < this.addresses.size(); _index++) {
			if (this.addresses.get(_index).getId().equalsIgnoreCase(address.getId())) {
				break;
			}
		}
		this.addresses.set(_index, address);
	}
	
	public boolean removeAddress(AddressModel address) {
		return this.addresses.remove(address);
	}
	
	public void setCreatedAt(Date createdAt) {
		model.setCreatedAt(createdAt);
	}
	
	public void setCreatedBy(String createdBy) {
		model.setCreatedBy(createdBy);
	}
	
	public void setModifiedAt(Date modifiedAt) {
		model.setModifiedAt(modifiedAt);		
	}
	
	public void setModifiedBy(String modifiedBy) {
		model.setModifiedBy(modifiedBy);
	}

	/**
	 * @return a list of Addressbook ids that contain this org
	 */
	public ArrayList<String> getMemberships() {
		return memberships;
	}
	
	public boolean isMemberOfAddressbook(String aid) {
		return memberships.contains(aid);
	}

	public boolean addMembership(String aid) {
		if (isMemberOfAddressbook(aid)) {
			return false;
		}
		memberships.add(aid);
		return true;
	}
	
	public boolean removeMembership(String aid) {
		return memberships.remove(aid);
	}
	
}

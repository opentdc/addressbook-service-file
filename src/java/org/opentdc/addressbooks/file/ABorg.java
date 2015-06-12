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

import org.opentdc.addressbooks.AddressModel;
import org.opentdc.addressbooks.OrgModel;

public class ABorg {
	private OrgModel model;
	private ArrayList<AddressModel> addresses;
	
	public ABorg() {
		addresses = new ArrayList<AddressModel>();
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
	
	public boolean removeAddress(AddressModel address) {
		return this.addresses.remove(address);
	}
}

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.opentdc.addressbooks.AddressModel;
import org.opentdc.addressbooks.AddressbookModel;
import org.opentdc.addressbooks.ContactModel;
import org.opentdc.addressbooks.ServiceProvider;
import org.opentdc.file.AbstractFileServiceProvider;
import org.opentdc.service.exception.DuplicateException;
import org.opentdc.service.exception.InternalServerErrorException;
import org.opentdc.service.exception.NotFoundException;
import org.opentdc.util.PrettyPrinter;

public class FileServiceProvider extends AbstractFileServiceProvider<ABadressbook> implements ServiceProvider {

	private static Map<String, ABadressbook> abookIndex = null;
	private static Map<String, ABcontact> contactIndex = null;
	private static Map<String, AddressModel> addressIndex = null;
	private static final Logger logger = Logger.getLogger(ServiceProvider.class.getName());
	
	public FileServiceProvider(
			ServletContext context, 
			String prefix
		) throws IOException {
		super(context, prefix);
		if (abookIndex == null) {
			abookIndex = new HashMap<String, ABadressbook>();
			contactIndex = new HashMap<String, ABcontact>();
			addressIndex = new HashMap<String, AddressModel>();
				
			List<ABadressbook> _addressbooks = importJson();
			for (ABadressbook _addressbook : _addressbooks) {
				addAbookToIndex(_addressbook);
			}
		}
	}
	
	@Override
	public List<AddressbookModel> list(
		String queryType,
		String query,
		long position,
		long size
	) {
		logger.info("list() -> " + count() + " addressbooks.");
		// Collections.sort(addressbooks, AddressbookModel.AdressbookComparator);
		List<AddressbookModel> _list = new ArrayList<AddressbookModel>();
		for (ABadressbook _adb : abookIndex.values()) {
			logger.info(PrettyPrinter.prettyPrintAsJSON(_adb.getAddressbookModel()));
			_list.add(_adb.getAddressbookModel());			
		}
		return _list;
	}

	@Override
	public AddressbookModel create(
		AddressbookModel addressbook
	) throws DuplicateException {
		logger.info("create(" + PrettyPrinter.prettyPrintAsJSON(addressbook) + ")");
		String _id = addressbook.getId();
		if (_id == null || _id == "") {
			_id = UUID.randomUUID().toString();
		} else {
			if (abookIndex.get(_id) != null) {
				// object with same ID exists already
				throw new DuplicateException("addressbook <" + _id + "> exists already.");
			}
		}
		addressbook.setId(_id);
		ABadressbook _adb = new ABadressbook();
		_adb.setAddressbookModel(addressbook);
		abookIndex.put(_id, _adb);
		logger.info("create() -> " + PrettyPrinter.prettyPrintAsJSON(_adb));
		if (isPersistent) {
			exportJson(abookIndex.values());
		}
		return _adb.getAddressbookModel();
	}

	@Override
	public AddressbookModel read(
		String id
	) throws NotFoundException {
		AddressbookModel _adbm = readAddressbook(id).getAddressbookModel();
		logger.info("read(" + id + ") -> " + PrettyPrinter.prettyPrintAsJSON(_adbm));
		return _adbm;
	}
	
	private ABadressbook readAddressbook(
			String id
	) throws NotFoundException {
		ABadressbook _adb = abookIndex.get(id);
		if (_adb == null) {
			throw new NotFoundException("addressbook <" + id
					+ "> was not found.");
		}
		logger.info("readAddressbook(" + id + ") -> " + PrettyPrinter.prettyPrintAsJSON(_adb));
		return _adb;
	}
	
	@Override
	public AddressbookModel update(
		String id,
		AddressbookModel addressbook
	) throws NotFoundException {
		ABadressbook _adb = abookIndex.get(id);
		if (_adb == null) {
			throw new NotFoundException("addressbook <" + id + "> was not found.");
		} else {
			_adb.getAddressbookModel().setName(addressbook.getName());
		}
		logger.info("update(" + id + ", " + PrettyPrinter.prettyPrintAsJSON(addressbook) + ") -> " +
				PrettyPrinter.prettyPrintAsJSON(_adb.getAddressbookModel()));
		if (isPersistent) {
			exportJson(abookIndex.values());
		}
		return _adb.getAddressbookModel();
	}

	@Override
	public void delete(
		String id
	) throws NotFoundException {
		ABadressbook _obj = abookIndex.get(id);

		if (_obj == null) {
			throw new NotFoundException("addressbook <" + id + "> was not found.");
		}
		for (ABcontact _contact : _obj.getContacts()) {
			removeContactFromIndex(_contact);
		}
		if (abookIndex.remove(id) == null) {
			throw new InternalServerErrorException("addressbook <" + id
					+ "> can not be removed, because it does not exist in the index");
		}
		if (isPersistent) {
			exportJson(abookIndex.values());
		}
		logger.info("delete(" + id + ")");
	}

	@Override
	public int count() {
		int _count = -1;
		if (abookIndex.size() != 0) {
			_count = abookIndex.size();
		}
		logger.info("count() = " + _count);
		return _count;
	}
	
	/******************************** contact *****************************************/
	@Override
	public ArrayList<ContactModel> listContacts(
			String aid,
			String query, 
			String queryType, 
			int position, 
			int size
	) {
		ArrayList<ContactModel> _contacts = new ArrayList<ContactModel>();
		for (ABcontact _c : readAddressbook(aid).getContacts()) {
			_contacts.add(_c.getContactModel());
		}
		// Collections.sort(_contacts, ContactModel.ContactComparator);
		logger.info("listContacts(" + aid + ") -> " + _contacts.size()
				+ " values");
		return _contacts;
	}
	
	@Override
	public ContactModel createContact(
		String aid, 
		ContactModel contact
	) throws DuplicateException {
		ABcontact _contact = createABContact(contact);
		readAddressbook(aid).addContact(_contact);
		logger.info("createContact(" + aid + ", " + PrettyPrinter.prettyPrintAsJSON(_contact.getContactModel()) + ")");
		if (isPersistent) {
			exportJson(abookIndex.values());
		}
		return _contact.getContactModel();
	}
	
	private ABcontact createABContact(
			ContactModel contact)
			throws DuplicateException {
		String _id = contact.getId();
		if (_id == null || _id == "") {
			_id = UUID.randomUUID().toString();
		} else {
			if (contactIndex.get(_id) != null) {
				// project with same ID exists already
				throw new DuplicateException("Contact <" + contact.getId() + 
						"> exists already.");
			}
		}
		contact.setId(_id);
		ABcontact _contact = new ABcontact();
		_contact.setContactModel(contact);
		addContactToIndex(_contact);
		return _contact;
	}

	@Override
	public ContactModel readContact(
			String cid) 
				throws NotFoundException {
		ContactModel _contact = readABcontact(cid).getContactModel();
		logger.info("readContact(" + cid + ") -> "
				+ PrettyPrinter.prettyPrintAsJSON(_contact));
		return _contact;
	}

	private ABcontact readABcontact(
			String cid)
				throws NotFoundException {
		ABcontact _c = contactIndex.get(cid);
		if (_c == null) {
			throw new NotFoundException("Contact <" + cid
					+ "> was not found.");
		}
		return _c;
	}
	
	@Override
	public ContactModel updateContact(
			String aid, 
			String cid,
			ContactModel contact) 
				throws NotFoundException {
		ABcontact _contact = contactIndex.get(cid);
		if (_contact == null) {
			throw new NotFoundException("contact <" + cid + "> not found");
		} else {
			_contact.getContactModel().setBirthday(contact.getBirthday());
			_contact.getContactModel().setCompany(contact.getCompany());
			_contact.getContactModel().setDepartment(contact.getDepartment());
			_contact.getContactModel().setFirstName(contact.getFirstName());
			_contact.getContactModel().setFn(contact.getFn());
			_contact.getContactModel().setJobTitle(contact.getJobTitle());
			_contact.getContactModel().setLastName(contact.getLastName());
			_contact.getContactModel().setMaidenName(contact.getMaidenName());
			_contact.getContactModel().setMiddleName(contact.getMiddleName());
			_contact.getContactModel().setNickName(contact.getNickName());
			_contact.getContactModel().setNote(contact.getNote());
			_contact.getContactModel().setPhotoUrl(contact.getPhotoUrl());
			_contact.getContactModel().setPrefix(contact.getPrefix());
			_contact.getContactModel().setSuffix(contact.getSuffix());
		}
		logger.info("updateContact(" + aid + ", " + cid + ", "+ PrettyPrinter.prettyPrintAsJSON(_contact) + ") -> OK");
		if (isPersistent) {
			exportJson(abookIndex.values());
		}
		return _contact.getContactModel();
	}

	@Override
	public void deleteContact(
			String aid, 
			String cid) 
				throws NotFoundException,
					InternalServerErrorException {
		removeContactFromIndex(readABcontact(cid));
					
		logger.info("deleteContact(" + aid + ", " + cid + ") -> OK");
		if (isPersistent) {
			exportJson(abookIndex.values());
		}
	}

	@Override
	public int countContacts(
		String aid) {
		int _count = readAddressbook(aid).getContacts().size();
		logger.info("countAddresses(" + aid + ") -> " + _count);
		return _count;
	}
	
	/******************************** address *****************************************/	
	@Override
	public List<AddressModel> listAddresses(String aid, String cid,
			String query, String queryType, int position, int size) {
		ArrayList<AddressModel> _addresses = readABcontact(cid).getAddresses();
		// Collections.sort(_addresses, AddressModel.ContactComparator);
		logger.info("listAddresses(" + aid + ", " + cid + ", " + query + ", " + 
				queryType + ", " + position + ", " + size + ") -> " + _addresses.size()	+ " values");
		return _addresses;
	}

	@Override
	public AddressModel createAddress(String aid, String cid,
			AddressModel address) throws DuplicateException {
		String _id = address.getId();
		if (_id == null || _id == "") {
			_id = UUID.randomUUID().toString();
		} else {
			if (addressIndex.get(_id) != null) {
				// address with same ID exists already
				throw new DuplicateException("Address <" + _id + "> exists already.");
			}
		}
		address.setId(_id);
		addressIndex.put(_id, address);
		readABcontact(cid).addAddress(address);
		logger.info("createAddress(" + aid + ", " + cid + ", "+ PrettyPrinter.prettyPrintAsJSON(address) + ")");
		if (isPersistent) {
			exportJson(abookIndex.values());
		}
		return address;
	}

	@Override
	public AddressModel readAddress(
			String aid, 
			String cid, 
			String adrid)
					throws NotFoundException {
		AddressModel _address = addressIndex.get(adrid);
		if (_address == null) {
			throw new NotFoundException("Address <" + adrid + "> was not found.");
		}
		logger.info("readAddress(" + aid + ", " + cid + ", " + adrid + ") -> " +
				PrettyPrinter.prettyPrintAsJSON(_address));
		return _address;
	}

	@Override
	public AddressModel updateAddress(
			String aid, 
			String cid, 
			String adrid,
			AddressModel address) 
				throws NotFoundException {
		AddressModel _address = readAddress(aid, cid, adrid);
		_address.setAttributeType(address.getAttributeType());
		_address.setMsgType(address.getMsgType());
		_address.setType(address.getType());
		_address.setValue(address.getValue());
		_address.setStreet(address.getStreet());
		_address.setPostalCode(address.getPostalCode());
		_address.setCity(address.getCity());
		_address.setCountry(address.getCountry());
		return _address;
	}

	@Override
	public void deleteAddress(
			String aid, 
			String cid, 
			String adrid)
			throws NotFoundException, InternalServerErrorException {
		if (addressIndex.remove(adrid) == null) {
			throw new InternalServerErrorException("address <" + adrid
					+ "> can not be removed, because it does not exist in the index");	
		}
		logger.info("deleteAddress(" + aid + ", " + cid + ", " + adrid + ") -> OK");
		if (isPersistent) {
			exportJson(abookIndex.values());
		}
	}
	
	@Override
	public int countAddresses(String aid, String cid) {
		int _count = readABcontact(cid).getAddresses().size();
		logger.info("countAddresses(" + aid + ", " + cid + ") -> " + _count);
		return _count;
	}
		
	/******************************** utility methods *****************************************/
	private void addAbookToIndex(
			ABadressbook abook) {
		abookIndex.put(abook.getAddressbookModel().getId(), abook);
		for (ABcontact _contact : abook.getContacts()) {
			addContactToIndex(_contact);
		}
	}
	
	private void addContactToIndex(
			ABcontact contact) {
		if (contact != null) {
			for (AddressModel _address : contact.getAddresses()) {
				addressIndex.put(_address.getId(), _address);
			}
		}
	}

	private void removeContactFromIndex(
			ABcontact contact) {
		if (contact != null) {
			for (AddressModel _address : contact.getAddresses()) {
				if (addressIndex.remove(_address.getId()) == null) {
					throw new InternalServerErrorException("address <" + _address.getId()
							+ "> can not be removed, because it does not exist in the index");	
				}
			}
			if ((contactIndex.remove(contact.getContactModel().getId())) == null) {
			throw new InternalServerErrorException("contact <" + contact.getContactModel().getId()
				+ "> can not be removed, because it does not exist in the index");
			}
		}
	}
}

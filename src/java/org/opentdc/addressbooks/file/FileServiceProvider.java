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
import java.util.Date;
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
import org.opentdc.service.exception.NotAllowedException;
import org.opentdc.service.exception.NotFoundException;
import org.opentdc.service.exception.ValidationException;
import org.opentdc.util.PrettyPrinter;

public class FileServiceProvider extends AbstractFileServiceProvider<ABaddressbook> implements ServiceProvider {

	private static Map<String, ABaddressbook> abookIndex = null;
	private static Map<String, ABcontact> contactIndex = null;
	private static Map<String, AddressModel> addressIndex = null;
	private static final Logger logger = Logger.getLogger(ServiceProvider.class.getName());
	
	public FileServiceProvider(
			ServletContext context, 
			String prefix
		) throws IOException {
		super(context, prefix);
		if (abookIndex == null) {
			abookIndex = new HashMap<String, ABaddressbook>();
			contactIndex = new HashMap<String, ABcontact>();
			addressIndex = new HashMap<String, AddressModel>();
				
			List<ABaddressbook> _addressbooks = importJson();
			for (ABaddressbook _addressbook : _addressbooks) {
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
		// Collections.sort(addressbooks, AddressbookModel.AdressbookComparator);
		List<AddressbookModel> _list = new ArrayList<AddressbookModel>();
		for (ABaddressbook _adb : abookIndex.values()) {
			logger.info(PrettyPrinter.prettyPrintAsJSON(_adb.getModel()));
			_list.add(_adb.getModel());			
		}
		logger.info("list() -> " + _list.size() + " addressbooks.");
		return _list;
	}

	@Override
	public AddressbookModel create(
		AddressbookModel addressbook
	) throws DuplicateException, ValidationException {
		logger.info("create(" + PrettyPrinter.prettyPrintAsJSON(addressbook) + ")");
		String _id = addressbook.getId();
		if (_id == null || _id == "") {
			_id = UUID.randomUUID().toString();
		} else {
			if (abookIndex.get(_id) != null) {
				// object with same ID exists already
				throw new DuplicateException("addressbook <" + _id + "> exists already.");
			}
			else {  // a new ID was set on the client; we do not allow this
				throw new ValidationException("addressbook <" + _id + 
						"> contains an ID generated on the client. This is not allowed.");
			}
		}
		addressbook.setId(_id);
		Date _date = new Date();
		addressbook.setCreatedAt(_date);
		addressbook.setCreatedBy("DUMMY_USER");
		addressbook.setModifiedAt(_date);
		addressbook.setCreatedBy("DUMMY_USER");
		abookIndex.put(_id, new ABaddressbook(addressbook));
		logger.info("create() -> " + PrettyPrinter.prettyPrintAsJSON(addressbook));
		if (isPersistent) {
			exportJson(abookIndex.values());
		}
		return addressbook;
	}

	@Override
	public AddressbookModel read(
		String id
	) throws NotFoundException {
		AddressbookModel _adbm = readAddressbook(id).getModel();
		logger.info("read(" + id + ") -> " + PrettyPrinter.prettyPrintAsJSON(_adbm));
		return _adbm;
	}
	
	private ABaddressbook readAddressbook(
			String id
	) throws NotFoundException {
		ABaddressbook _adb = abookIndex.get(id);
		if (_adb == null) {
			throw new NotFoundException("addressbook <" + id
					+ "> was not found.");
		}
		logger.info("readAddressbook(" + id + ") -> " + PrettyPrinter.prettyPrintAsJSON(_adb));
		return _adb;
	}
	
	@Override
	public AddressbookModel update(
		String aid,
		AddressbookModel addressbook
	) throws NotFoundException, NotAllowedException {
		ABaddressbook _adb = readAddressbook(aid);
		AddressbookModel _am = _adb.getModel();
		if (! _am.getCreatedAt().equals(addressbook.getCreatedAt())) {
			throw new NotAllowedException("addressbook<" + aid + ">: it is not allowed to change createdAt on the client.");
		}
		if (! _am.getCreatedBy().equalsIgnoreCase(addressbook.getCreatedBy())) {
			throw new NotAllowedException("addressbook<" + aid + ">: it is not allowed to change createdBy on the client.");
		}
		_am.setName(addressbook.getName());
		_am.setModifiedAt(new Date());
		_am.setModifiedBy("DUMMY_USER");
		_adb.setModel(_am);

		logger.info("update(" + aid + ", " + PrettyPrinter.prettyPrintAsJSON(addressbook) + ") -> " +
				PrettyPrinter.prettyPrintAsJSON(_adb.getModel()));
		if (isPersistent) {
			exportJson(abookIndex.values());
		}
		return _adb.getModel();
	}

	@Override
	public void delete(
		String id
	) throws NotFoundException {
		ABaddressbook _adb = readAddressbook(id);
		for (ABcontact _contact : _adb.getContacts()) {
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
			_contacts.add(_c.getModel());
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
		String _id = contact.getId();
		if (_id == null || _id == "") {
			_id = UUID.randomUUID().toString();
		} else {
			if (contactIndex.get(_id) != null) {
				// project with same ID exists already
				throw new DuplicateException("contact <" + contact.getId() + 
						"> exists already.");
			}
			else {  // a new ID was set on the client; we do not allow this
				throw new ValidationException("contact <" + _id +
						"> contains an ID generated on the client. This is not allowed.");
			}
		}
		contact.setId(_id);
		Date _date = new Date();
		contact.setCreatedAt(_date);
		contact.setCreatedBy("DUMMY_USER");
		contact.setModifiedAt(_date);
		contact.setCreatedBy("DUMMY_USER");
		
		ABcontact _contact = new ABcontact();
		_contact.setModel(contact);
		addContactToIndex(_contact);		
		readAddressbook(aid).addContact(_contact);
		logger.info("createContact(" + aid + ", " + PrettyPrinter.prettyPrintAsJSON(_contact.getModel()) + ")");
		if (isPersistent) {
			exportJson(abookIndex.values());
		}
		return _contact.getModel();
	}
		
	private ABcontact readABcontact(
			String id)
		throws NotFoundException {
		ABcontact _c = contactIndex.get(id);
		if (_c == null) {
			throw new NotFoundException("contact <" + id + "> was not found.");			
		}
		return _c;
	}

	@Override
	public ContactModel readContact(
			String aid,
			String cid) 
				throws NotFoundException {
		readAddressbook(aid);		// verify existence of addressbook
		ABcontact _c = readABcontact(cid);
		logger.info("readContact(" + aid + ", " + cid + ") -> "
				+ PrettyPrinter.prettyPrintAsJSON(_c.getModel()));
		return _c.getModel();
	}
	
	@Override
	public ContactModel updateContact(
			String aid, 
			String cid,
			ContactModel contact) 
				throws NotFoundException, NotAllowedException 
	{
		readAddressbook(aid);		// verify existence of addressbook
		ABcontact _c = readABcontact(cid);
		ContactModel _cm = _c.getModel();
		
		if (! _cm.getCreatedAt().equals(contact.getCreatedAt())) {
			throw new NotAllowedException("contact<" + cid + ">: it is not allowed to change createdAt on the client.");
		}
		if (! _cm.getCreatedBy().equalsIgnoreCase(contact.getCreatedBy())) {
			throw new NotAllowedException("contact<" + cid + ">: it is not allowed to change createdBy on the client.");
		}
		_cm.setPhotoUrl(contact.getPhotoUrl());
		_cm.setFn(contact.getFn());
		_cm.setFirstName(contact.getFirstName());
		_cm.setLastName(contact.getLastName());
		_cm.setMiddleName(contact.getMiddleName());
		_cm.setMaidenName(contact.getMaidenName());
		_cm.setPrefix(contact.getPrefix());
		_cm.setSuffix(contact.getSuffix());
		_cm.setNickName(contact.getNickName());
		_cm.setJobTitle(contact.getJobTitle());
		_cm.setDepartment(contact.getDepartment());
		_cm.setCompany(contact.getCompany());
		_cm.setBirthday(contact.getBirthday());
		_cm.setNote(contact.getNote());
		_cm.setModifiedAt(new Date());
		_cm.setModifiedBy("DUMMY_USER");
		_c.setModel(_cm);
		logger.info("updateContact(" + aid + ", " + cid + ", "+ PrettyPrinter.prettyPrintAsJSON(_cm) + ") -> OK");
		if (isPersistent) {
			exportJson(abookIndex.values());
		}
		return _cm;
	}

	@Override
	public void deleteContact(
			String aid, 
			String cid) 
				throws NotFoundException,
					InternalServerErrorException 
	{
		readAddressbook(aid);		// verify existence of addressbook
		ABcontact _c = readABcontact(cid);
		removeContactFromIndex(_c);
					
		logger.info("deleteContact(" + aid + ", " + cid + ") -> OK");
		if (isPersistent) {
			exportJson(abookIndex.values());
		}
	}
	
	/******************************** address *****************************************/	
	@Override
	public List<AddressModel> listAddresses(
			String aid, 
			String cid,
			String query, 
			String queryType, 
			int position, 
			int size) {
		readAddressbook(aid);		// verify existence of addressbook
		ABcontact _c = readABcontact(cid);
		ArrayList<AddressModel> _addresses = _c.getAddresses();
		// Collections.sort(_addresses, AddressModel.ContactComparator);
		logger.info("listAddresses(" + aid + ", " + cid + ", " + query + ", " + 
				queryType + ", " + position + ", " + size + ") -> " + _addresses.size()	+ " values");
		return _addresses;
	}

	@Override
	public AddressModel createAddress(
			String aid, 
			String cid,
			AddressModel address) 
					throws DuplicateException {
		readAddressbook(aid);		// verify existence of addressbook
		ABcontact _c = readABcontact(cid);
		String _id = address.getId();
		if (_id == null || _id == "") {
			_id = UUID.randomUUID().toString();
		} else {
			if (addressIndex.get(_id) != null) {
				// address with same ID exists already
				throw new DuplicateException("address <" + _id + "> exists already.");
			}
			else {  // a new ID was set on the client; we do not allow this
				throw new ValidationException("address <" + _id +
						"> contains an ID generated on the client. This is not allowed.");
			}
		}
		address.setId(_id);
		Date _date = new Date();
		address.setCreatedAt(_date);
		address.setCreatedBy("DUMMY_USER");
		address.setModifiedAt(_date);
		address.setModifiedBy("DUMMY_USER");
		addressIndex.put(_id, address);
		_c.addAddress(address);
		logger.info("createAddress(" + aid + ", " + cid + ", "+ PrettyPrinter.prettyPrintAsJSON(address) + ")");
		if (isPersistent) {
			exportJson(abookIndex.values());
		}
		return address;
	}
	
	private AddressModel getAddress(String id) {
		AddressModel _address = addressIndex.get(id);
		if (_address == null) {
			throw new NotFoundException("address <" + id + "> was not found.");
		}		
		return _address;
	}
	
	@Override
	public AddressModel readAddress(
			String aid, 
			String cid, 
			String adrid)
					throws NotFoundException {
		readAddressbook(aid);		// verify existence of addressbook
		readABcontact(cid);			// verify existence of contact
		AddressModel _address = getAddress(adrid);
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
		readAddressbook(aid);		// verify existence of addressbook
		ABcontact _c = readABcontact(cid);			// verify existence of contact
		AddressModel _am = getAddress(adrid);
		if (! _am.getCreatedAt().equals(address.getCreatedAt())) {
			throw new NotAllowedException("contact<" + cid + ">: it is not allowed to change createdAt on the client.");
		}
		if (! _am.getCreatedBy().equalsIgnoreCase(address.getCreatedBy())) {
			throw new NotAllowedException("contact<" + cid + ">: it is not allowed to change createdBy on the client.");
		}

		_am.setAttributeType(address.getAttributeType());
		_am.setType(address.getType());
		_am.setMsgType(address.getMsgType());
		_am.setValue(address.getValue());
		_am.setStreet(address.getStreet());
		_am.setPostalCode(address.getPostalCode());
		_am.setCity(address.getCity());
		_am.setCountry(address.getCountry());
		_am.setModifiedAt(new Date());
		_am.setModifiedBy("DUMMY_USER");
		_c.addAddress(_am);
		logger.info("updateAddress(" + aid + ", " + cid + ", " + adrid + ") -> " +
				PrettyPrinter.prettyPrintAsJSON(_am));
		if (isPersistent) {
			exportJson(abookIndex.values());
		}
		return _am;
	}

	@Override
	public void deleteAddress(
			String aid, 
			String cid, 
			String adrid)
			throws NotFoundException, InternalServerErrorException {
		readAddressbook(aid);		// verify existence of addressbook
		ABcontact _c = readABcontact(cid);			// verify existence of contact
		AddressModel _adr = getAddress(adrid);
		
		if (_c.removeAddress(_adr) == false) {
			throw new InternalServerErrorException("address <" + adrid + "> could not be removed from contact <" 
					+ cid + ">, because it was not listed as a member of the contact.");
		}
		if (addressIndex.remove(adrid) == null) {
			throw new InternalServerErrorException("address <" + adrid
					+ "> can not be removed, because it does not exist in the index");	
		}
		logger.info("deleteAddress(" + aid + ", " + cid + ", " + adrid + ") -> OK");
		if (isPersistent) {
			exportJson(abookIndex.values());
		}
	}
	
	/******************************** utility methods *****************************************/
	private void addAbookToIndex(
			ABaddressbook abook) {
		abookIndex.put(abook.getModel().getId(), abook);
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
			contactIndex.put(contact.getModel().getId(), contact);
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
			if ((contactIndex.remove(contact.getModel().getId())) == null) {
			throw new InternalServerErrorException("contact <" + contact.getModel().getId()
				+ "> can not be removed, because it does not exist in the index");
			}
		}
	}
}

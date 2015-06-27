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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.opentdc.addressbooks.AddressModel;
import org.opentdc.addressbooks.AddressbookModel;
import org.opentdc.addressbooks.ContactModel;
import org.opentdc.addressbooks.OrgModel;
import org.opentdc.addressbooks.OrgType;
import org.opentdc.addressbooks.ServiceProvider;
import org.opentdc.file.AbstractFileServiceProvider;
import org.opentdc.service.exception.DuplicateException;
import org.opentdc.service.exception.InternalServerErrorException;
import org.opentdc.service.exception.NotFoundException;
import org.opentdc.service.exception.ValidationException;
import org.opentdc.util.PrettyPrinter;

public class FileServiceProvider extends AbstractFileServiceProvider<ABaddressbook> implements ServiceProvider {

	private static Map<String, ABaddressbook> abookIndex = null;
	private static Map<String, ABcontact> contactIndex = null;
	private static Map<String, ABorg> orgIndex = null;
	private static Map<String, AddressModel> addressIndex = null;
	private static final Logger logger = Logger.getLogger(ServiceProvider.class.getName());
	
	public FileServiceProvider(
			ServletContext context, 
			String prefix
		) throws IOException {
		super(context, prefix);
		if (abookIndex == null) {
			abookIndex = new ConcurrentHashMap<String, ABaddressbook>();
			contactIndex = new ConcurrentHashMap<String, ABcontact>();
			orgIndex = new ConcurrentHashMap<String, ABorg>();
			addressIndex = new ConcurrentHashMap<String, AddressModel>();
			
			List<ABaddressbook> _addressbooks = importJson();
			for (ABaddressbook _addressbook : _addressbooks) {
				addAbookToIndex(_addressbook);
			}
		}
		logger.info("indexed " 
			+ abookIndex.size() + " AddressBooks, "
			+ contactIndex.size() + " Contacts, "
			+ orgIndex.size() + " Organizations, "
			+ addressIndex.size() + " Addresses.");
	}
	
	@Override
	public List<AddressbookModel> list(
		String query,
		String queryType,
		int position,
		int size
	) {
		ArrayList<AddressbookModel> _addressbooks = new ArrayList<AddressbookModel>();
		for (ABaddressbook _ab : abookIndex.values()) {
			_addressbooks.add(_ab.getModel());
		}
		Collections.sort(_addressbooks, AddressbookModel.AddressbookComparator);
		ArrayList<AddressbookModel> _selection = new ArrayList<AddressbookModel>();
		for (int i = 0; i < _addressbooks.size(); i++) {
			if (i >= position && i < (position + size)) {
				_selection.add(_addressbooks.get(i));
			}			
		}
		logger.info("list(<" + query + ">, <" + queryType + 
			">, <" + position + ">, <" + size + ">) -> " + _selection.size() + " addressbooks.");
		return _selection;
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
		if (addressbook.getName() == null || addressbook.getName().length() == 0) {
			throw new ValidationException("addressbook <" + _id + 
					"> must contain a valid name.");
		}
		addressbook.setId(_id);
		Date _date = new Date();
		addressbook.setCreatedAt(_date);
		addressbook.setCreatedBy("DUMMY_USER");
		addressbook.setModifiedAt(_date);
		addressbook.setModifiedBy("DUMMY_USER");
		abookIndex.put(_id, new ABaddressbook(addressbook));
		logger.info("create() -> " + PrettyPrinter.prettyPrintAsJSON(addressbook));
		exportJson(abookIndex.values());
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
		// beware: this log entry is bad for performance as it is called many times
		// logger.info("readAddressbook(" + id + ") -> " + PrettyPrinter.prettyPrintAsJSON(_adb));
		return _adb;
	}
	
	@Override
	public AddressbookModel update(
		String aid,
		AddressbookModel addressbook
	) throws NotFoundException, ValidationException {
		ABaddressbook _adb = readAddressbook(aid);
		AddressbookModel _am = _adb.getModel();
		if (! _am.getCreatedAt().equals(addressbook.getCreatedAt())) {
			throw new ValidationException("addressbook<" + aid + ">: it is not allowed to change createdAt on the client.");
		}
		if (! _am.getCreatedBy().equalsIgnoreCase(addressbook.getCreatedBy())) {
			throw new ValidationException("addressbook<" + aid + ">: it is not allowed to change createdBy on the client.");
		}
		_am.setName(addressbook.getName());
		_am.setModifiedAt(new Date());
		_am.setModifiedBy("DUMMY_USER");
		_adb.setModel(_am);

		logger.info("update(" + aid + ", " + PrettyPrinter.prettyPrintAsJSON(addressbook) + ") -> " +
				PrettyPrinter.prettyPrintAsJSON(_adb.getModel()));
		exportJson(abookIndex.values());
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
		exportJson(abookIndex.values());
		logger.info("delete(" + id + ")");
	}

	@Override
	public ArrayList<ContactModel> listAllContacts(
			String query, 
			String queryType, 
			int position, 
			int size
	) {
		ArrayList<ContactModel> _contacts = new ArrayList<ContactModel>(); 
		for (ABaddressbook _ab : abookIndex.values()) {
			for (ABcontact _c : _ab.getContacts()) {
				_contacts.add(_c.getModel());
			}
		}

		Collections.sort(_contacts, ContactModel.ContactComparator);
		ArrayList<ContactModel> _selection = new ArrayList<ContactModel>(); 
		for (int i = 0; i < _contacts.size(); i++) {
			if (i >= position && i < (position + size)) {
				_selection.add(_contacts.get(i));
			}
		}
		logger.info("listAllContacts(<" + query + ">, <" + queryType + 
				">, <" + position + ">, <" + size + ">) -> " + _selection.size()
				+ " values");
		return _selection;
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
		Collections.sort(_contacts, ContactModel.ContactComparator);
		ArrayList<ContactModel> _selection = new ArrayList<ContactModel>(); 
		for (int i = 0; i < _contacts.size(); i++) {
			if (i >= position && i < (position + size)) {
				_selection.add(_contacts.get(i));
			}
		}
		logger.info("listContacts(<" + aid + ">, <" + query + ">, <" + queryType + 
				">, <" + position + ">, <" + size + ">) -> " + _selection.size()
				+ " values");
		return _selection;
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
				// contact with same ID exists already
				throw new DuplicateException("contact <" + contact.getId() + 
						"> exists already.");
			}
			else {  // a new ID was set on the client; we do not allow this
				throw new ValidationException("contact <" + _id +
						"> contains an ID generated on the client. This is not allowed.");
			}
		}
		if(
			(contact.getFirstName() == null || contact.getFirstName().isEmpty()) &&
			(contact.getLastName() == null || contact.getLastName().isEmpty())
		) {
			throw new ValidationException("contact must contain a fn name.");
		}
		String _fn = contact.getFirstName();
		if (_fn == null || _fn.isEmpty()) {
			_fn = contact.getLastName();
		} else {
			if (contact.getLastName() != null && !contact.getLastName().isEmpty()) {
				_fn = _fn + " " + contact.getLastName();
			}
		}
		contact.setFn(_fn);
		contact.setId(_id);
		Date _date = new Date();
		contact.setCreatedAt(_date);
		contact.setCreatedBy("DUMMY_USER");
		contact.setModifiedAt(_date);
		contact.setModifiedBy("DUMMY_USER");
		
		ABcontact _contact = new ABcontact();
		_contact.setModel(contact);
		addContactToIndex(_contact);		
		readAddressbook(aid).addContact(_contact);
		logger.info("createContact(" + aid + ", " + PrettyPrinter.prettyPrintAsJSON(_contact.getModel()) + ")");
		exportJson(abookIndex.values());
		return _contact.getModel();
	}
		
	/**
	 * @param id
	 * @return
	 * @throws NotFoundException
	 */
	private ABcontact readABcontact(
			String id)
		throws NotFoundException {
		ABcontact _c = contactIndex.get(id);
		if (_c == null) {
			throw new NotFoundException("contact <" + id + "> was not found.");			
		}
		return _c;
	}

	/* (non-Javadoc)
	 * @see org.opentdc.addressbooks.ServiceProvider#readContact(java.lang.String, java.lang.String)
	 */
	@Override
	public ContactModel readContact(
			String aid,
			String cid) 
				throws NotFoundException {
		readAddressbook(aid);		// verify existence of addressbook
		ABcontact _abContact = readABcontact(cid);
		logger.info("readContact(" + aid + ", " + cid + ") -> "
				+ PrettyPrinter.prettyPrintAsJSON(_abContact.getModel()));
		return _abContact.getModel();
	}
	
	@Override
	public ContactModel updateContact(
			String aid, 
			String cid,
			ContactModel contact) 
				throws NotFoundException, ValidationException 
	{
		readAddressbook(aid);		// verify existence of addressbook
		ABcontact _c = readABcontact(cid);
		ContactModel _cm = _c.getModel();
		
		if (! _cm.getCreatedAt().equals(contact.getCreatedAt())) {
			throw new ValidationException("contact<" + cid + ">: it is not allowed to change createdAt on the client.");
		}
		if (! _cm.getCreatedBy().equalsIgnoreCase(contact.getCreatedBy())) {
			throw new ValidationException("contact<" + cid + ">: it is not allowed to change createdBy on the client.");
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
		exportJson(abookIndex.values());
		return _cm;
	}

	@Override
	public void deleteContact(
			String aid, 
			String cid) 
				throws NotFoundException,
					InternalServerErrorException 
	{
		ABaddressbook _abab = readAddressbook(aid);		// verify existence of addressbook
		ABcontact _c = readABcontact(cid);
		if (_abab.removeContact(_c) == false) {
			throw new InternalServerErrorException("contact <" + cid + "> could not be removed from addressbook <"
				+ aid + ">, because it was not listed as a member of the addressbook.");
		}
		removeContactFromIndex(_c);
					
		logger.info("deleteContact(" + aid + ", " + cid + ") -> OK");
		exportJson(abookIndex.values());
	}

	/******************************** org *****************************************/
	@Override
	public List<OrgModel> listOrgs(String aid, String query, String queryType,
			int position, int size) {
		ArrayList<OrgModel> _orgs = new ArrayList<OrgModel>(); 
		for (ABorg _o : readAddressbook(aid).getOrgs()) {
			_orgs.add(_o.getModel());
		}
		Collections.sort(_orgs, OrgModel.OrgComparator);
		ArrayList<OrgModel> _selection = new ArrayList<OrgModel>(); 
		for (int i = 0; i < _orgs.size(); i++) {
			if (i >= position && i < (position + size)) {
				_selection.add(_orgs.get(i));
			}
		}
		logger.info("listOrgs(<" + aid + ">, <" + query + ">, <" + queryType + 
				">, <" + position + ">, <" + size + ">) -> " + _selection.size()
				+ " values");
		return _selection;
	}

	@Override
	public OrgModel createOrg(String aid, OrgModel org)
			throws DuplicateException, ValidationException {
		String _id = org.getId();
		if (_id == null || _id == "") {
			_id = UUID.randomUUID().toString();
		} else {
			if (orgIndex.get(_id) != null) {
				// org with same ID exists already
				throw new DuplicateException("org <" + org.getId() + 
						"> exists already.");
			}
			else {  // a new ID was set on the client; we do not allow this
				throw new ValidationException("org <" + _id +
						"> contains an ID generated on the client. This is not allowed.");
			}
		}
		if (org.getName() == null || org.getName().length() == 0) {
			throw new ValidationException("org <" + _id + 
						"> must contain a name.");
		}
		if (org.getOrgType() == null) {
			org.setOrgType(OrgType.getDefaultOrgType());
		}
		org.setId(_id);
		Date _date = new Date();
		org.setCreatedAt(_date);
		org.setCreatedBy("DUMMY_USER");
		org.setModifiedAt(_date);
		org.setModifiedBy("DUMMY_USER");
		
		ABorg _aborg = new ABorg();
		_aborg.setModel(org);
		orgIndex.put(_id, _aborg);

		readAddressbook(aid).addOrg(_aborg);
		logger.info("createOrg(" + aid + ", " + PrettyPrinter.prettyPrintAsJSON(_aborg.getModel()) + ")");
		exportJson(abookIndex.values());
		return _aborg.getModel();
	}

	private ABorg readABorg(
			String oid)
		throws NotFoundException {
		ABorg _aborg = orgIndex.get(oid);
		if (_aborg == null) {
			throw new NotFoundException("org <" + oid + "> was not found.");			
		}
		return _aborg;
	}
	
	@Override
	public OrgModel readOrg(
			String aid, 
			String oid) 
					throws NotFoundException {
		readAddressbook(aid);		// verify existence of addressbook
		ABorg _abOrg = readABorg(oid);
		logger.info("readOrg(" + aid + ", " + oid + ") -> "
				+ PrettyPrinter.prettyPrintAsJSON(_abOrg.getModel()));
		return _abOrg.getModel();
	}

	@Override
	public OrgModel updateOrg(
			String aid, 
			String oid, 
			OrgModel org)
			throws NotFoundException, ValidationException {
		readAddressbook(aid);		// verify existence of addressbook
		ABorg _abOrg = readABorg(oid);
		OrgModel _om = _abOrg.getModel();
		
		if (! _om.getCreatedAt().equals(org.getCreatedAt())) {
			throw new ValidationException("contact<" + oid + ">: it is not allowed to change createdAt on the client.");
		}
		if (! _om.getCreatedBy().equalsIgnoreCase(org.getCreatedBy())) {
			throw new ValidationException("contact<" + oid + ">: it is not allowed to change createdBy on the client.");
		}
		_om.setName(org.getName());
		_om.setDescription(org.getDescription());
		_om.setCostCenter(org.getCostCenter());
		_om.setStockExchange(org.getStockExchange());
		_om.setTickerSymbol(org.getTickerSymbol());
		_om.setOrgType(org.getOrgType());
		_om.setLogoUrl(org.getLogoUrl());
		_om.setModifiedAt(new Date());
		_om.setModifiedBy("DUMMY_USER");
		_abOrg.setModel(_om);
		logger.info("updateOrg(" + aid + ", " + oid + ", "+ PrettyPrinter.prettyPrintAsJSON(_om) + ") -> OK");
		exportJson(abookIndex.values());
		return _om;
	}

	@Override
	public void deleteOrg(
			String aid, 
			String oid) 
					throws NotFoundException,
			InternalServerErrorException {
		ABaddressbook _abab = readAddressbook(aid);		// verify existence of addressbook
		ABorg _abOrg = readABorg(oid);
		if (_abab.removeOrg(_abOrg) == false) {
			throw new InternalServerErrorException("org <" + oid + "> could not be removed from addressbook <"
				+ aid + ">, because it was not listed as a member of the addressbook.");
		}
		removeOrgFromIndex(_abOrg);
					
		logger.info("deleteContact(" + aid + ", " + oid + ") -> OK");
		exportJson(abookIndex.values());
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
		List<AddressModel> _addresses = _c.getAddresses();
		Collections.sort(_addresses, AddressModel.AddressComparator);
		ArrayList<AddressModel> _selection = new ArrayList<AddressModel>();
		for (int i = 0; i < _addresses.size(); i++) {
			if (i >= position && i < (position + size)) {
				_selection.add(_addresses.get(i));
			}
		}		
		logger.info("listAddresses(" + aid + ", " + cid + ", " + query + ", " + 
				queryType + ", " + position + ", " + size + ") -> " + _selection.size()	+ " values");
		return _selection;
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
		if (address.getAttributeType() == null || address.getAttributeType().length() == 0) {
			throw new ValidationException("address <" + _id + 
					"> must contain an attributeType.");
		}
		if (address.getType() == null) {
			throw new ValidationException("address <" + _id + 
					"> must contain a type.");
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
		exportJson(abookIndex.values());
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
				throws NotFoundException, ValidationException {
		readAddressbook(aid);		// verify existence of addressbook
		ABcontact _c = readABcontact(cid);			// verify existence of contact
		AddressModel _am = getAddress(adrid);
		if (! _am.getCreatedAt().equals(address.getCreatedAt())) {
			throw new ValidationException("contact<" + cid + ">: it is not allowed to change createdAt on the client.");
		}
		if (! _am.getCreatedBy().equalsIgnoreCase(address.getCreatedBy())) {
			throw new ValidationException("contact<" + cid + ">: it is not allowed to change createdBy on the client.");
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
		exportJson(abookIndex.values());
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
		exportJson(abookIndex.values());
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
	
	private void removeOrgFromIndex(ABorg org) {
		if (org != null) {
			for (AddressModel _address : org.getAddresses()) {
				if (addressIndex.remove(_address.getId()) == null) {
					throw new InternalServerErrorException("address <" + _address.getId()
							+ "> can not be removed, because it does not exist in the index");
				}
			}
			if ((orgIndex.remove(org.getModel().getId())) == null) {
				throw new InternalServerErrorException("org <" + org.getModel().getId()
						+ "> can not be removed, because it does not exist in the index");
			}
		}
	}
}

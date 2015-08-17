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
	private static ABaddressbook allAddressbook = null;
	private static final String ALL_ADDRESSBOOK_NAME = "AAA";
	
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
				if (_addressbook.getModel().getName().equalsIgnoreCase(ALL_ADDRESSBOOK_NAME)) {
					allAddressbook = _addressbook;
				}
			}
			if (_addressbooks.size() == 0) {
				// create implicit 'all' addressbook
				AddressbookModel _am = new AddressbookModel();
				_am.setId(UUID.randomUUID().toString());
				_am.setName(ALL_ADDRESSBOOK_NAME);
				Date _date = new Date();
				_am.setCreatedAt(_date);
				_am.setCreatedBy("SYSTEM");
				_am.setModifiedAt(_date);
				_am.setModifiedBy("SYSTEM");	
				allAddressbook = new ABaddressbook(_am);
				abookIndex.put(_am.getId(), allAddressbook);
				logger.info("create() -> " + PrettyPrinter.prettyPrintAsJSON(_am));
				exportJson(abookIndex.values());
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

	/**
	 * Creates a new addressbook. Addressbooks are grouping contacts and orgs; they are a subset of all contacts or orgs.
	 * The addressbook 'all' is implicit. It can not be created nor updated or deleted. Its contacts can be listed with allContacts resp. allOrgs.
	 * @param addressbook the new addressbook data
	 * @return the newly created addressbook; this is the addressbook parameter data plus a newly created id.
	 * @throws DuplicateException if an addressbook with the same id already exists
	 * @throws ValidationException if the addressbook contains an id generated on the client, if the mandatory name is missing or if the reserved name 'all' is used.");
	 */
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
		if (addressbook.getName().equalsIgnoreCase(ALL_ADDRESSBOOK_NAME)) {
			throw new ValidationException("[" + ALL_ADDRESSBOOK_NAME + "] is a reserved addressbook name; please choose a different name.");
		}
		addressbook.setId(_id);
		Date _date = new Date();
		addressbook.setCreatedAt(_date);
		addressbook.setCreatedBy(getPrincipal());
		addressbook.setModifiedAt(_date);
		addressbook.setModifiedBy(getPrincipal());
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
			logger.warning("addressbook<" + aid + ">: ignoring createdAt value <" + addressbook.getCreatedAt().toString() + 
					"> because it was set on the client");
		}
		if (! _am.getCreatedBy().equalsIgnoreCase(addressbook.getCreatedBy())) {
			logger.warning("addressbook<" + aid + ">: ignoring createdBy value <" + addressbook.getCreatedBy() +
					"> because it was set on the client.");
		}
		if (addressbook.getName() == null || addressbook.getName().length() == 0) {
			throw new ValidationException("new values of addressbook <" + aid + 
					"> must contain a valid name.");
		}
		if (addressbook.getName().equalsIgnoreCase(ALL_ADDRESSBOOK_NAME)) {
			throw new ValidationException("[" + ALL_ADDRESSBOOK_NAME + "] is a reserved name for addressbooks; please choose a different name.");
		}
		_am.setName(addressbook.getName());
		_am.setModifiedAt(new Date());
		_am.setModifiedBy(getPrincipal());
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
		for (String _cid : _adb.getContacts()) {
			removeContactFromIndex(_cid);
		}
		for (String _oid : _adb.getOrgs()) {
			removeOrgFromIndex(_oid);
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
		for (ABcontact _abContact : contactIndex.values()) {
			_contacts.add(_abContact.getModel());
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

	@Override
	public ArrayList<OrgModel> listAllOrgs(
			String query, 
			String queryType, 
			int position, 
			int size
	) {
		ArrayList<OrgModel> _orgs = new ArrayList<OrgModel>(); 
		for (ABorg _abOrg : orgIndex.values()) {
			_orgs.add(_abOrg.getModel());
		}

		Collections.sort(_orgs, OrgModel.OrgComparator);
		ArrayList<OrgModel> _selection = new ArrayList<OrgModel>(); 
		for (int i = 0; i < _orgs.size(); i++) {
			if (i >= position && i < (position + size)) {
				_selection.add(_orgs.get(i));
			}
		}
		logger.info("listAllOrgs(<" + query + ">, <" + queryType + 
				">, <" + position + ">, <" + size + ">) -> " + _selection.size()
				+ " values");
		return _selection;
	}

	/******************************** contact *****************************************/
	/* (non-Javadoc)
	 * @see org.opentdc.addressbooks.ServiceProvider#listContacts(java.lang.String, java.lang.String, java.lang.String, int, int)
	 */
	@Override
	public ArrayList<ContactModel> listContacts(
			String aid,
			String query, 
			String queryType, 
			int position, 
			int size) 
	{
		ArrayList<ContactModel> _contacts = new ArrayList<ContactModel>(); 
		for (String _cid : readAddressbook(aid).getContacts()) {
			_contacts.add(readABcontact(_cid).getModel());
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
	
	/* (non-Javadoc)
	 * @see org.opentdc.addressbooks.ServiceProvider#createContact(java.lang.String, org.opentdc.addressbooks.ContactModel)
	 */
	@Override
	public ContactModel createContact(
		String aid, 
		ContactModel contact) 
				throws DuplicateException, ValidationException 
	{
		// logger.info("createContact(" + aid + ", " + PrettyPrinter.prettyPrintAsJSON(contact) + ")");
		String _id = contact.getId();
		if (_id == null || _id == "") {
			_id = UUID.randomUUID().toString();
			contact.setId(_id);
			String _fn = ContactModel.createFullName(contact.getFirstName(), contact.getLastName());
			if (_fn == null) {
				throw new ValidationException("contact <" + _id + 
						"> must contain either a valid firstName and/or a valid lastName");
			}
			contact.setFn(_fn);
			Date _date = new Date();
			contact.setCreatedAt(_date);
			contact.setCreatedBy(getPrincipal());
			contact.setModifiedAt(_date);
			contact.setModifiedBy(getPrincipal());

			ABcontact _abContact = new ABcontact();
			_abContact.setModel(contact);
			_abContact.addMembership(aid);
			readAddressbook(aid).addContact(_id);
			if (!allAddressbook.getModel().getId().equalsIgnoreCase(aid)) {	// custom addressbook
				_abContact.addMembership(allAddressbook.getModel().getId());
				allAddressbook.addContact(_id);
			}
			addContactToIndex(_abContact);	
		} 
		else {
			ABcontact _contact = contactIndex.get(_id);
			if (_contact != null) {		// same contact exists in index already
				ABaddressbook _ab = readAddressbook(aid);
				if (_ab.containsContact(_id) == true) {
					throw new DuplicateException("contact <" + contact.getId() + 
						"> exists already.");
				} else {		// add the existing contact to this addressbook
					_contact.addMembership(aid);
					_ab.addContact(_id);
				}	
			}
			else {  // a new ID was set on the client; we do not allow this
				throw new ValidationException("contact <" + _id +
						"> contains an ID generated on the client. This is not allowed.");
			}
		}
		logger.info("createContact(" + aid + ", contact) -> " + PrettyPrinter.prettyPrintAsJSON(contact));
		exportJson(abookIndex.values());
		return contact;
	}
	
	/* (non-Javadoc)
	 * @see org.opentdc.addressbooks.ServiceProvider#readContact(java.lang.String, java.lang.String)
	 */
	@Override
	public ContactModel readContact(
			String aid,
			String cid) 
				throws NotFoundException {
		ABaddressbook _abAddressbook = readAddressbook(aid);		// verify existence of addressbook
		if (_abAddressbook.containsContact(cid) == false) {
			throw new NotFoundException("contact <" + cid + "> was not found in Addressbook <" + aid +">.");
		}
		return getContactModel(cid);
	}
	
	public static ContactModel getContactModel(
			String contactId)
			throws NotFoundException {
		ABcontact _abContact = readABcontact(contactId);
		logger.info("getContactModel(" + contactId + ") -> "
				+ PrettyPrinter.prettyPrintAsJSON(_abContact.getModel()));
		return _abContact.getModel();
	}
	
	/**
	 * @param id
	 * @return
	 * @throws NotFoundException
	 */
	private static ABcontact readABcontact(
			String id)
		throws NotFoundException {
		ABcontact _c = contactIndex.get(id);
		if (_c == null) {
			throw new NotFoundException("contact <" + id + "> was not found.");			
		}
		return _c;
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
			logger.warning("contact <" + cid + ">: ignoring createdAt value <" + contact.getCreatedAt().toString() +
					"> because it was set on the client.");
		}
		if (! _cm.getCreatedBy().equalsIgnoreCase(contact.getCreatedBy())) {
			logger.warning("contact <" + cid + ">: ignoring createdBy value <" + contact.getCreatedBy() + 
					"> because it was set on the client.");
		}
		String _fn = ContactModel.createFullName(contact.getFirstName(), contact.getLastName());
		if (_fn == null) {
			throw new ValidationException("contact <" + cid + 
					"> must contain either a valid firstName and/or a valid lastName");
		}
		_cm.setFn(_fn);
		_cm.setPhotoUrl(contact.getPhotoUrl());
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
		_cm.setModifiedBy(getPrincipal());
		_c.setModel(_cm);
		logger.info("updateContact(" + aid + ", " + cid + ", "+ PrettyPrinter.prettyPrintAsJSON(_cm) + ") -> OK");
		exportJson(abookIndex.values());
		return _cm;
	}
	
	// deleting a contact from a custom addressbook -> remove it from the addressbook, but keep it in all addressbook
	// deleting a contact from all addressbook -> remove it from all custom addressbooks
	/* (non-Javadoc)
	 * @see org.opentdc.addressbooks.ServiceProvider#deleteContact(java.lang.String, java.lang.String)
	 */
	@Override
	public void deleteContact(
			String aid, 
			String cid) 
				throws NotFoundException,
					InternalServerErrorException 
	{
		ABaddressbook _abab = readAddressbook(aid);		// verify existence of addressbook
		ABcontact _contact = readABcontact(cid);		// throws NotFoundException
		if (aid.equalsIgnoreCase(allAddressbook.getModel().getId())) { // all addressbook -> full delete
			for (String _aid : _contact.getMemberships()) {
				readAddressbook(_aid).removeContact(cid);
			}
			removeContactFromIndex(cid);			
		} else {		// delete from custom addressbook
			if (_abab.removeContact(cid) == false) {
				throw new NotFoundException("contact <" + cid + "> was not found in addressbook <" + aid +">.");
			}
			_contact.removeMembership(aid);
		}
					
		logger.info("deleteContact(" + aid + ", " + cid + ") -> OK");
		exportJson(abookIndex.values());
	}

	/******************************** org *****************************************/
	/* (non-Javadoc)
	 * @see org.opentdc.addressbooks.ServiceProvider#listOrgs(java.lang.String, java.lang.String, java.lang.String, int, int)
	 */
	@Override
	public List<OrgModel> listOrgs(
			String aid, 
			String query, 
			String queryType,
			int position, 
			int size) 
	{
		ArrayList<OrgModel> _orgs = new ArrayList<OrgModel>(); 
		for (String _oid : readAddressbook(aid).getOrgs()) {
			_orgs.add(readABorg(_oid).getModel());
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

	/* (non-Javadoc)
	 * @see org.opentdc.addressbooks.ServiceProvider#createOrg(java.lang.String, org.opentdc.addressbooks.OrgModel)
	 */
	@Override
	public OrgModel createOrg(
			String aid, 
			OrgModel org)
					throws DuplicateException, ValidationException 
	{
		String _id = org.getId();
		if (_id == null || _id == "") {
			_id = UUID.randomUUID().toString();
			org.setId(_id);
			if (org.getName() == null || org.getName().length() == 0) {
				throw new ValidationException("org <" + _id + "> must contain a name.");
			}
			if (org.getOrgType() == null) {
				org.setOrgType(OrgType.getDefaultOrgType());
			}
			Date _date = new Date();
			org.setCreatedAt(_date);
			org.setCreatedBy(getPrincipal());
			org.setModifiedAt(_date);
			org.setModifiedBy(getPrincipal());
			
			ABorg _abOrg = new ABorg();
			_abOrg.setModel(org);
			_abOrg.addMembership(aid);
			readAddressbook(aid).addOrg(_id);
			if (!allAddressbook.getModel().getId().equalsIgnoreCase(aid)) {	// custom addressbook
				_abOrg.addMembership(allAddressbook.getModel().getId());	
				allAddressbook.addOrg(_id);
			}
			addOrgToIndex(_abOrg);	
		} else {
			ABorg _org = orgIndex.get(_id);
			if (_org != null) {	// same org exists in index already
				ABaddressbook _ab = readAddressbook(aid);
				if (_ab.containsOrg(_id) == true) {
					throw new DuplicateException("org <" + org.getId() + 
							"> exists already.");
				} else {	// add the existing org to this addressbook
					_org.addMembership(aid);
					_ab.addOrg(_id);
				}
			}
			else {  // a new ID was set on the client; we do not allow this
				throw new ValidationException("org <" + _id +
						"> contains an ID generated on the client. This is not allowed.");
			}
		}
		logger.info("createOrg(" + aid + ", " + PrettyPrinter.prettyPrintAsJSON(org) + ")");
		exportJson(abookIndex.values());
		return org;
	}

	/**
	 * @param oid
	 * @return
	 * @throws NotFoundException
	 */
	private ABorg readABorg(
			String oid)
		throws NotFoundException {
		ABorg _aborg = orgIndex.get(oid);
		if (_aborg == null) {
			throw new NotFoundException("org <" + oid + "> was not found.");			
		}
		return _aborg;
	}
	
	/* (non-Javadoc)
	 * @see org.opentdc.addressbooks.ServiceProvider#readOrg(java.lang.String, java.lang.String)
	 */
	@Override
	public OrgModel readOrg(
			String aid, 
			String oid) 
					throws NotFoundException {
		ABaddressbook _abAddressbook = readAddressbook(aid);		// verify existence of addressbook
		if (_abAddressbook.containsOrg(oid) == false) {
			throw new NotFoundException("contact <" + oid + "> was not found in Addressbook <" + aid +">.");
		}
		ABorg _abOrg = readABorg(oid);
		logger.info("readOrg(" + aid + ", " + oid + ") -> "
				+ PrettyPrinter.prettyPrintAsJSON(_abOrg.getModel()));
		return _abOrg.getModel();
	}

	/* (non-Javadoc)
	 * @see org.opentdc.addressbooks.ServiceProvider#updateOrg(java.lang.String, java.lang.String, org.opentdc.addressbooks.OrgModel)
	 */
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
			logger.warning("contact<" + oid + ">: ignoring createdAt value <" + org.getCreatedAt().toString() +
					"> because it was set on the client.");
		}
		if (! _om.getCreatedBy().equalsIgnoreCase(org.getCreatedBy())) {
			logger.warning("contact<" + oid + ">: ignoring createdBy value <" + org.getCreatedBy() +
					"> because it was set on the client.");
		}
		if (org.getName() == null || org.getName().length() == 0) {
			throw new ValidationException("org <" + oid + "> must contain a name.");
		}
		if (org.getOrgType() == null) {
			org.setOrgType(OrgType.getDefaultOrgType());
		}
		_om.setName(org.getName());
		_om.setDescription(org.getDescription());
		_om.setCostCenter(org.getCostCenter());
		_om.setStockExchange(org.getStockExchange());
		_om.setTickerSymbol(org.getTickerSymbol());
		_om.setOrgType(org.getOrgType());
		_om.setLogoUrl(org.getLogoUrl());
		_om.setModifiedAt(new Date());
		_om.setModifiedBy(getPrincipal());
		_abOrg.setModel(_om);
		logger.info("updateOrg(" + aid + ", " + oid + ", "+ PrettyPrinter.prettyPrintAsJSON(_om) + ") -> OK");
		exportJson(abookIndex.values());
		return _om;
	}

	/* (non-Javadoc)
	 * @see org.opentdc.addressbooks.ServiceProvider#deleteOrg(java.lang.String, java.lang.String)
	 */
	@Override
	public void deleteOrg(
			String aid, 
			String oid) 
					throws NotFoundException,
					InternalServerErrorException 
	{
		ABaddressbook _abab = readAddressbook(aid);		// verify existence of addressbook
		ABorg _org = readABorg(oid);
		if (aid.equalsIgnoreCase(allAddressbook.getModel().getId())) {	// all addressbook -> full delete
			for (String _aid : _org.getMemberships()) {
				readAddressbook(_aid).removeOrg(oid);
			}
			removeOrgFromIndex(oid);
		} else {		// delete from custom addressbook
			if (_abab.removeOrg(oid) == false) {
				throw new NotFoundException("org <" + oid + "> was not found in addressbook <" + aid +">.");
			}		
			_org.removeMembership(aid);
		}
					
		logger.info("deleteOrg(" + aid + ", " + oid + ") -> OK");
		exportJson(abookIndex.values());
	}
	
	/******************************** address (of contacts) *****************************************/	
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
					throws ValidationException, DuplicateException {
		readAddressbook(aid);		// verify existence of addressbook
		ABcontact _contact = readABcontact(cid);
		AddressModel _newAddress = validateNewAddress(address);
		addressIndex.put(_newAddress.getId(), _newAddress);
		_contact.addAddress(_newAddress);
		logger.info("createAddress(" + aid + ", " + cid + ", "+ PrettyPrinter.prettyPrintAsJSON(address) + ")");
		exportJson(abookIndex.values());
		return _newAddress;
	}
	
	private AddressModel validateNewAddress(
			AddressModel address) 
				throws ValidationException, DuplicateException {
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
		if (address.getAddressType() == null) {
			throw new ValidationException("address <" + _id + "> must contain an addressType.");
		}
		if (address.getAttributeType() == null) {
			throw new ValidationException("address <" + _id + "> must contain an attributeType.");					
		}
		switch(address.getAddressType()) {
			case PHONE:
			case EMAIL:
			case WEB:
				if (address.getValue() == null || address.getValue().length() == 0) {
					throw new ValidationException(address.getAddressType().toString() + " address <" + _id + 
							"> must contain a value.");					
				}
				address.setMsgType(null);
				address.setStreet(null);
				address.setPostalCode(null);
				address.setCity(null);
				address.setCountryCode((short) 0);
				break;
			case MESSAGING:
				if (address.getValue() == null || address.getValue().length() == 0) {
					throw new ValidationException(address.getAddressType().toString() + " address <" + _id + 
							"> must contain a value.");					
				}
				if (address.getMsgType() == null) {
					throw new ValidationException(address.getAddressType().toString() + " address <" + _id + 
							"> must contain a messageType.");										
				}
				address.setStreet(null);
				address.setPostalCode(null);
				address.setCity(null);
				address.setCountryCode((short) 0);				
				break;
			case POSTAL:		// no mandatory fields
				address.setMsgType(null);
				address.setValue(null);
				break;
			default:
				throw new ValidationException("address <" + _id + 
						"> can not be created, because it contains an invalid addressType: " + 
						address.getAddressType());
		}
		address.setId(_id);
		Date _date = new Date();
		address.setCreatedAt(_date);
		address.setCreatedBy(getPrincipal());
		address.setModifiedAt(_date);
		address.setModifiedBy(getPrincipal());
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
		ABcontact _abContact = readABcontact(cid);			// verify existence of contact
		AddressModel _am = validateChangedAddress("contact", cid, adrid, address);
		addressIndex.put(adrid, _am);
		_abContact.replaceAddress(_am);
		logger.info("updateAddress(" + aid + ", " + cid + ", " + adrid + ") -> " +
				PrettyPrinter.prettyPrintAsJSON(_am));
		exportJson(abookIndex.values());
		return _am;
	}
	
	private AddressModel validateChangedAddress(
			String parentType, 
			String pid, 
			String adrid, 
			AddressModel address) 
					throws NotFoundException, ValidationException {
		AddressModel _am = getAddress(adrid);
		if (! _am.getCreatedAt().equals(address.getCreatedAt())) {
			logger.warning(parentType + " <" + pid + ">: ignoring createdAt value <" + address.getCreatedAt().toString() + 
					"> because it was set on the client.");
		}
		if (! _am.getCreatedBy().equalsIgnoreCase(address.getCreatedBy())) {
			logger.warning(parentType + " <" + pid + ">: ignoring createdBy value <" + address.getCreatedBy() +
					"> because it was set on the client.");
		}
		if (address.getAddressType() == null) {
			throw new ValidationException("address <" + adrid + 
					"> can not be updated, because the new address must contain an addressType.");
		}
		if (address.getAddressType() != _am.getAddressType()) {
			throw new ValidationException("address <" + adrid +
					"> can not be updated, because it is not allowed to change the AddressType.");
		}
		if (address.getAttributeType() == null) {
			throw new ValidationException("address <" + adrid + 
					"> can not be updated, because the new address must contain an attributeType.");					
		}
		switch(address.getAddressType()) {
		case PHONE:
		case EMAIL:
		case WEB:
			if (address.getValue() == null || address.getValue().length() == 0) {
				throw new ValidationException(address.getAddressType().toString() + " address <" + adrid + 
						"> can not be updated, because the new address must contain a value.");					
			}
			_am.setAttributeType(address.getAttributeType());
			_am.setValue(address.getValue());
			break;
		case MESSAGING:
			if (address.getValue() == null || address.getValue().length() == 0) {
				throw new ValidationException(address.getAddressType().toString() + " address <" + adrid + 
						"> can not be updated, because the new address must contain a value.");					
			}
			if (address.getMsgType() == null) {
				throw new ValidationException(address.getAddressType().toString() + " address <" + adrid + 
						"> can not be updated, because the new address must contain a msgType.");										
			}
			_am.setAttributeType(address.getAttributeType());
			_am.setMsgType(address.getMsgType());
			_am.setValue(address.getValue());
			break;
		case POSTAL:		// no mandatory fields
			_am.setStreet(address.getStreet());
			_am.setPostalCode(address.getPostalCode());
			_am.setCity(address.getCity());
			_am.setCountryCode(address.getCountryCode());
			break;
		default:
			throw new ValidationException("address <" + adrid + 
					"> can not be updated, because the new address has an invalid addressType: " + address.getAddressType().toString());
		}
		_am.setModifiedAt(new Date());
		_am.setModifiedBy(getPrincipal());
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

	/******************************** address (of orgs) *****************************************/	
	@Override
	public List<AddressModel> listOrgAddresses(
			String aid, 
			String oid,
			String query, 
			String queryType, 
			int position, 
			int size) {
		readAddressbook(aid);		// verify existence of addressbook
		ABorg _org = readABorg(oid);
		List<AddressModel> _addresses = _org.getAddresses();
		Collections.sort(_addresses, AddressModel.AddressComparator);
		ArrayList<AddressModel> _selection = new ArrayList<AddressModel>();
		for (int i = 0; i < _addresses.size(); i++) {
			if (i >= position && i < (position + size)) {
				_selection.add(_addresses.get(i));
			}
		}		
		logger.info("listAddresses(" + aid + ", " + oid + ", " + query + ", " + 
				queryType + ", " + position + ", " + size + ") -> " + _selection.size()	+ " values");
		return _selection;
	}

	@Override
	public AddressModel createOrgAddress(
			String aid, 
			String oid,
			AddressModel address) 
					throws ValidationException, DuplicateException {
		readAddressbook(aid);		// verify existence of addressbook
		ABorg _org = readABorg(oid);
		AddressModel _newAddress = validateNewAddress(address);
		addressIndex.put(_newAddress.getId(), _newAddress);
		_org.addAddress(_newAddress);
		logger.info("createAddress(" + aid + ", " + oid + ", "+ PrettyPrinter.prettyPrintAsJSON(address) + ")");
		exportJson(abookIndex.values());
		return _newAddress;
	}
	
	@Override
	public AddressModel readOrgAddress(
			String aid, 
			String oid, 
			String adrid)
					throws NotFoundException {
		readAddressbook(aid);		// verify existence of addressbook
		readABorg(oid);			// verify existence of org
		AddressModel _address = getAddress(adrid);
		logger.info("readAddress(" + aid + ", " + oid + ", " + adrid + ") -> " +
				PrettyPrinter.prettyPrintAsJSON(_address));
		return _address;
	}

	@Override
	public AddressModel updateOrgAddress(
			String aid, 
			String oid, 
			String adrid,
			AddressModel address) 
				throws NotFoundException, ValidationException {
		readAddressbook(aid);		// verify existence of addressbook
		ABorg _abOrg = readABorg(oid);			// verify existence of org
		AddressModel _am = validateChangedAddress("org", oid, adrid, address);
		addressIndex.put(adrid, _am);
		_abOrg.replaceAddress(_am);
		logger.info("updateAddress(" + aid + ", " + oid + ", " + adrid + ") -> " +
				PrettyPrinter.prettyPrintAsJSON(_am));
		exportJson(abookIndex.values());
		return _am;
	}

	@Override
	public void deleteOrgAddress(
			String aid, 
			String oid, 
			String adrid)
			throws NotFoundException, InternalServerErrorException {
		readAddressbook(aid);		// verify existence of addressbook
		ABorg _org = readABorg(oid);			// verify existence of contact
		AddressModel _adr = getAddress(adrid);
		
		if (_org.removeAddress(_adr) == false) {
			throw new InternalServerErrorException("address <" + adrid + "> could not be removed from org <" 
					+ oid + ">, because it was not listed as a member of the org.");
		}
		if (addressIndex.remove(adrid) == null) {
			throw new InternalServerErrorException("address <" + adrid
					+ "> can not be removed, because it does not exist in the index");	
		}
		logger.info("deleteAddress(" + aid + ", " + oid + ", " + adrid + ") -> OK");
		exportJson(abookIndex.values());
	}
	
	
	/******************************** utility methods *****************************************/
	private void addAbookToIndex(
			ABaddressbook abook) {
		abookIndex.put(abook.getModel().getId(), abook);
		for (String _cid : abook.getContacts()) {
			addContactToIndex(readABcontact(_cid));
		}
		for (String _oid : abook.getOrgs()) {
			addOrgToIndex(readABorg(_oid));
		}
	}
	
	private void addContactToIndex(
			ABcontact abContact) {
		if (abContact != null) {
			for (AddressModel _address : abContact.getAddresses()) {
				addressIndex.put(_address.getId(), _address);
			}
			contactIndex.put(abContact.getModel().getId(), abContact);
		}
	}
	
	private void addOrgToIndex(
			ABorg abOrg) {
		if (abOrg != null) {
			for (AddressModel _address : abOrg.getAddresses()) {
				addressIndex.put(_address.getId(), _address);
			}
			orgIndex.put(abOrg.getModel().getId(), abOrg);
		}
	}
	
	private void removeContactFromIndex(
		String cid) 
	{
		if (cid != null) {
			ABcontact _abContact = readABcontact(cid);
			for (AddressModel _address : _abContact.getAddresses()) {
				if (addressIndex.remove(_address.getId()) == null) {
					throw new InternalServerErrorException("address <" + _address.getId()
							+ "> can not be removed, because it does not exist in the index");	
				}
			}
			if ((contactIndex.remove(cid)) == null) {
				throw new InternalServerErrorException("contact <" + cid
					+ "> can not be removed, because it does not exist in the index");
			}
			logger.info("removed contact <" + cid + "> from index.");
		}
	}
	
	private void removeOrgFromIndex(
			String oid) 
	{
		if (oid != null) {
			ABorg _abOrg = readABorg(oid);
			for (AddressModel _address : _abOrg.getAddresses()) {
				if (addressIndex.remove(_address.getId()) == null) {
					throw new InternalServerErrorException("address <" + _address.getId()
							+ "> can not be removed, because it does not exist in the index");
				}
			}
			if ((orgIndex.remove(oid)) == null) {
				throw new InternalServerErrorException("org <" + oid
						+ "> can not be removed, because it does not exist in the index");
			}				
			logger.info("removed org <" + oid + "> from index.");
		}
	}
	
	public static ABaddressbook getAllAddressbook() {
		return allAddressbook;
	}
}

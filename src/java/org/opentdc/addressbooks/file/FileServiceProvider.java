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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.opentdc.addressbooks.AddressbookModel;
import org.opentdc.addressbooks.ServiceProvider;
import org.opentdc.service.exception.DuplicateException;
import org.opentdc.service.exception.NotFoundException;

public class FileServiceProvider implements ServiceProvider {

	private static Map<String, AddressbookModel> data = new HashMap<String, AddressbookModel>();
	private static final Logger logger = Logger.getLogger(ServiceProvider.class.getName());

	public FileServiceProvider(
		ServletContext context, 
		String prefix
	) {
		logger.info("> FileServiceProvider()");
	}
	
	private void setNewID(AddressbookModel dataObj) {
		String _id = UUID.randomUUID().toString();
		dataObj.setId(_id);
	}

	private void storeData(AddressbookModel dataObj) {
		data.put(dataObj.getId(), dataObj);
	}

	private AddressbookModel getData(String id) {
		return data.get(id);
	}

	private List<AddressbookModel> getData() {
		return new ArrayList<AddressbookModel>(data.values());
	}

	private int getDataSize() {
		int _retVal = 0;
		if (data != null) {
			_retVal = data.size();
		}
		return _retVal;
	}

	private void removeData(String id) {
		data.remove(id);
	}

	@Override
	public List<AddressbookModel> list(
		String queryType,
		String query,
		long position,
		long size
	) {
		List<AddressbookModel> _list = getData();
		logger.info("list() -> " + getDataSize() + " values");
		return _list;
	}

	@Override
	public AddressbookModel create(
		AddressbookModel adddressbook
	) throws DuplicateException {
		if (getData(adddressbook.getId()) != null) {
			throw new DuplicateException();
		}

		// TODO: do we need to validate dataObj with BeanPropertyBindingResult ?
		// see example in LocationService

		setNewID(adddressbook);
		storeData(adddressbook);
		return adddressbook;
	}

	@Override
	public AddressbookModel read(
		String id
	) throws NotFoundException {
		AddressbookModel addressbook = getData(id);
		if (addressbook == null) {
			throw new NotFoundException();
		}
		// response.setId(id);
		logger.info("read(" + id + "): " + addressbook);
		return addressbook;
	}

	@Override
	public AddressbookModel update(
		String id,
		AddressbookModel addressbook
	) throws NotFoundException {
		if (getData(id) == null) {
			throw new NotFoundException();
		} else {
			storeData(addressbook);
			return addressbook;
		}
	}

	@Override
	public void delete(
		String id
	) throws NotFoundException {
		AddressbookModel _dataObj = getData(id);

		if (_dataObj == null) {
			throw new NotFoundException();
		}
		removeData(id);
		logger.info("delete(" + id + ")");
	}

	@Override
	public void deleteAll() {
		data.clear();
	}
		
	@Override
	public int count(
	) {
		return this.getDataSize();
	}
	
}

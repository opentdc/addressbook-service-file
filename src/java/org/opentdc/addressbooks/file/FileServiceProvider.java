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

import org.opentdc.addressbooks.AddressbookModel;
import org.opentdc.addressbooks.ServiceProvider;
import org.opentdc.file.AbstractFileServiceProvider;
import org.opentdc.service.exception.DuplicateException;
import org.opentdc.service.exception.NotFoundException;

public class FileServiceProvider extends AbstractFileServiceProvider<AddressbookModel> implements ServiceProvider {

	private static Map<String, AddressbookModel> index = new HashMap<String, AddressbookModel>();
	private static final Logger logger = Logger.getLogger(ServiceProvider.class.getName());
	
	public FileServiceProvider(
			ServletContext context, 
			String prefix
		) throws IOException {
			super(context, prefix);
			if (index == null) {
				index = new HashMap<String, AddressbookModel>();
				List<AddressbookModel> _addressbooks = importJson();
				for (AddressbookModel _addressbook : _addressbooks) {
					index.put(_addressbook.getId(), _addressbook);
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
		List<AddressbookModel> _list = new ArrayList<AddressbookModel>(index.values());
		logger.info("list() -> " + count() + " values");
		return _list;
	}

	@Override
	public AddressbookModel create(
		AddressbookModel addressbook
	) throws DuplicateException {
		logger.info("create(" + addressbook + ")");
		String _id = addressbook.getId();
		if (_id == null || _id == "") {
			_id = UUID.randomUUID().toString();
		} else {
			if (index.get(_id) != null) {
				// object with same ID exists already
				throw new DuplicateException();
			}
		}
		AddressbookModel _adb = new AddressbookModel();
		_adb.setId(_id);
		index.put(_id, _adb);
		if (isPersistent) {
			exportJson(index.values());
		}
		return _adb;
	}

	@Override
	public AddressbookModel read(
		String id
	) throws NotFoundException {
		AddressbookModel addressbook = index.get(id);;
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
		if (index.get(id) == null) {
			throw new NotFoundException();
		} else {
			index.put(addressbook.getId(), addressbook);
			if (isPersistent) {
				exportJson(index.values());
			}
			return addressbook;
		}
	}

	@Override
	public void delete(
		String id
	) throws NotFoundException {
		AddressbookModel _dataObj = index.get(id);

		if (_dataObj == null) {
			throw new NotFoundException();
		}
		index.remove(id);
		if (isPersistent) {
			exportJson(index.values());
		}
		logger.info("delete(" + id + ")");
	}

	@Override
	public int count() {
		int _retVal = 0;
		if (index != null) {
			_retVal = index.size();
		}
		return _retVal;
	}
}

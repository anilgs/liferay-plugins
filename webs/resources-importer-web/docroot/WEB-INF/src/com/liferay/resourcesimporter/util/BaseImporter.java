/**
 * Copyright (c) 2000-2012 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.resourcesimporter.util;

import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.PropertiesUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.GroupConstants;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.LayoutConstants;
import com.liferay.portal.model.LayoutSetPrototype;
import com.liferay.portal.model.LayoutTypePortlet;
import com.liferay.portal.model.User;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.LayoutSetPrototypeLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.UserLocalServiceUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;

/**
 * @author Brian Wing Shun Chan
 * @author Raymond Augé
 */
public abstract class BaseImporter implements Importer {

	public void afterPropertiesSet() throws Exception {
		settingsProperties = new Properties();

		try {
			InputStream inputStream = null;

			Class<?> clazz = getClass();

			if (clazz.isAssignableFrom(FileSystemImporter.class)) {
				inputStream = new FileInputStream(
					resourcesDir.concat("settings.properties"));
			}
			else {
				inputStream = servletContext.getResourceAsStream(
					resourcesDir.concat("settings.properties"));
			}

			if (inputStream != null) {
				String settingsString = StringUtil.read(inputStream);

				PropertiesUtil.load(settingsProperties, settingsString);
			}
		}
		catch (IOException e) {
			_log.error(e, e);
		}

		User user = UserLocalServiceUtil.getDefaultUser(companyId);

		userId = user.getUserId();

		Group group = null;

		if (targetClassName.equals(LayoutSetPrototype.class.getName())) {
			LayoutSetPrototype layoutSetPrototype = getLayoutSetPrototype(
				companyId, targetValue);

			if (layoutSetPrototype != null) {
				existing = true;
			}
			else {
				layoutSetPrototype =
					LayoutSetPrototypeLocalServiceUtil.addLayoutSetPrototype(
						userId, companyId, getTargetValueMap(),
						StringPool.BLANK, true, true, new ServiceContext());
			}

			group = layoutSetPrototype.getGroup();

			privateLayout = true;
			targetClassPK = layoutSetPrototype.getLayoutSetPrototypeId();
		}
		else if (targetClassName.equals(Group.class.getName())) {
			if (targetValue.equals(GroupConstants.GUEST)) {
				group = GroupLocalServiceUtil.getGroup(
					companyId, GroupConstants.GUEST);

				List<Layout> layouts = LayoutLocalServiceUtil.getLayouts(
					group.getGroupId(), false,
					LayoutConstants.DEFAULT_PARENT_LAYOUT_ID, false, 0, 1);

				if (!layouts.isEmpty()) {
					Layout layout = layouts.get(0);

					LayoutTypePortlet layoutTypePortlet =
						(LayoutTypePortlet)layout.getLayoutType();

					List<String> portletIds = layoutTypePortlet.getPortletIds();

					if (portletIds.size() != 2) {
						existing = true;
					}

					for (String portletId : portletIds) {
						if (!portletId.equals("47") &&
							!portletId.equals("58")) {

							existing = true;
						}
					}
				}
			}
			else {
				group = GroupLocalServiceUtil.fetchGroup(
					companyId, targetValue);

				if (group != null) {
					existing = true;
				}
				else {
					group = GroupLocalServiceUtil.addGroup(
						userId, GroupConstants.DEFAULT_PARENT_GROUP_ID,
						StringPool.BLANK, 0,
						GroupConstants.DEFAULT_LIVE_GROUP_ID, targetValue,
						StringPool.BLANK, GroupConstants.TYPE_SITE_OPEN, null,
						true, true, new ServiceContext());
				}
			}

			privateLayout = false;
			targetClassPK = group.getGroupId();
		}

		if (group != null) {
			groupId = group.getGroupId();
		}
	}

	public long getGroupId() {
		return groupId;
	}

	public Properties getSettingsProperties() {
		return settingsProperties;
	}

	public long getTargetClassPK() {
		return targetClassPK;
	}

	public Map<Locale, String> getTargetValueMap() {
		Map<Locale, String> targetValueMap = new HashMap<Locale, String>();

		Locale locale = LocaleUtil.getDefault();

		targetValueMap.put(locale, targetValue);

		return targetValueMap;
	}

	public boolean isExisting() {
		return existing;
	}

	public void setCompanyId(long companyId) {
		this.companyId = companyId;
	}

	public void setResourcesDir(String resourcesDir) {
		this.resourcesDir = resourcesDir;
	}

	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	public void setServletContextName(String servletContextName) {
		this.servletContextName = servletContextName;
	}

	public void setTargetClassName(String targetClassName) {
		this.targetClassName = targetClassName;
	}

	public void setTargetValue(String targetValue) {
		this.targetValue = targetValue;
	}

	protected LayoutSetPrototype getLayoutSetPrototype(
			long companyId, String name)
		throws Exception {

		Locale locale = LocaleUtil.getDefault();

		List<LayoutSetPrototype> layoutSetPrototypes =
			LayoutSetPrototypeLocalServiceUtil.search(
				companyId, null, QueryUtil.ALL_POS, QueryUtil.ALL_POS, null);

		for (LayoutSetPrototype layoutSetPrototype : layoutSetPrototypes) {
			if (name.equals(layoutSetPrototype.getName(locale))) {
				return layoutSetPrototype;
			}
		}

		return null;
	}

	protected long companyId;
	protected boolean existing;
	protected long groupId;
	protected boolean privateLayout;
	protected String resourcesDir;
	protected ServletContext servletContext;
	protected String servletContextName;
	protected Properties settingsProperties;
	protected String targetClassName;
	protected long targetClassPK;
	protected String targetValue;
	protected long userId;

	private static Log _log = LogFactoryUtil.getLog(BaseImporter.class);

}
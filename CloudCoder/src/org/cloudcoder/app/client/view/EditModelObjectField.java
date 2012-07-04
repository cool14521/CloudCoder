// CloudCoder - a web-based pedagogical programming environment
// Copyright (C) 2011-2012, Jaime Spacco <jspacco@knox.edu>
// Copyright (C) 2011-2012, David H. Hovemeyer <david.hovemeyer@gmail.com>
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package org.cloudcoder.app.client.view;

import com.google.gwt.user.client.ui.IsWidget;

/**
 * Visual editor for a field of a model object.
 * 
 * @author David Hovemeyer
 */
public abstract class EditModelObjectField<ModelObjectType, FieldType> {
	private final String desc;
	private ModelObjectType modelObj;
	
	/**
	 * Constructor.
	 * 
	 * @param desc the description that should be used to label the UI widget
	 */
	public EditModelObjectField(String desc) {
		this.desc = desc;
	}
	
	/**
	 * Get the description that should be used to label the UI widget.
	 * 
	 * @return the description that should be used to label the UI widget
	 */
	public String getDescription() {
		return desc;
	}
	
	/**
	 * Set the model object.
	 * Automatically calls {@link #update()} to force the editor
	 * to sync its contents with the model object's data.
	 * 
	 * @param modelObj the model object to set
	 */
	public void setModelObject(ModelObjectType modelObj) {
		this.modelObj = modelObj;
		update();
	}
	
	/**
	 * Get the model object.
	 * 
	 * @return the model object
	 */
	public ModelObjectType getModelObject() {
		return modelObj;
	}
	
	/**
	 * Get a widget for editing the field of the model object.
	 * 
	 * @return a widget for editing the field of the model object
	 */
	public abstract IsWidget getUI();
	
	/**
	 * Commit any changes made in the UI to the model object.
	 */
	public abstract void commit();
	
	/**
	 * Force the editor to refresh itself by synchronizing its
	 * UI state with the model object's state.  Call this method
	 * when the model object changes.
	 */
	public abstract void update();
	
	/**
	 * Downcall method to set the field in the model object.
	 * 
	 * @param value    the field value to set
	 */
	protected abstract void setField(FieldType value);
	
	/**
	 * Downcall method to get the value of the field in the model object.
	 * 
	 * @return the field value
	 */
	protected abstract FieldType getField();
}
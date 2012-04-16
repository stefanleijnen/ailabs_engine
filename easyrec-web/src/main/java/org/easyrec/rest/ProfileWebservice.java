/*
 * Copyright 2012 Research Studios Austria Forschungsgesellschaft mBH
 *
 * This file is part of easyrec.
 *
 * easyrec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * easyrec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with easyrec.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.easyrec.rest;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import com.sun.jersey.api.json.JSONWithPadding;
import com.sun.jersey.spi.resource.Singleton;
import org.easyrec.model.core.web.Message;
import org.easyrec.service.core.ProfileService;
import org.easyrec.store.dao.web.OperatorDAO;
import org.easyrec.vocabulary.MSG;
import org.easyrec.vocabulary.WS;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This class is a REST webservice facade for the ProfileService
 *
 * @author Fabian Salcher
 */

/*
* The path of the URI must begin with the version number of the API.
* Followed by an optional "json" part. If this part is missing the result
* will be returned as XML.
*/
@Path("1.0{responseType: (/json)?}/profile")
@Produces({"application/xml", "application/json"})
@Singleton

public class ProfileWebservice {

    @Context
    public HttpServletRequest request;

    private ProfileService profileService;
    private OperatorDAO operatorDAO;

    private final static String JAMON_PROFILE_LOAD = "rest.profile.load";
    private final static String JAMON_PROFILE_STORE = "rest.profile.store";
    private final static String JAMON_PROFILE_DELETE = "rest.profile.delete";
    private final static String JAMON_PROFILE_FIELD_STORE = "rest.profile.field.store";
    private final static String JAMON_PROFILE_FIELD_LOAD = "rest.profile.field.load";
    private final static String JAMON_PROFILE_FIELD_DELETE = "rest.profile.field.delete";

    public ProfileWebservice(ProfileService profileService, OperatorDAO operatorDAO) {
        this.profileService = profileService;
        this.operatorDAO = operatorDAO;
    }


    /**
     * This method stores the given profile to the item defined by the tenantID,
     * itemID and the itemTypeID. If there is already a profile it will be overwritten.
     *
     * @param responseType defines the media type of the result
     * @param apiKey       the apiKey which admits access to the API
     * @param tenantID     the tenantID of the item where the profile will be stored
     * @param itemID       the itemID if the item where the profile will be stored
     * @param itemType     the itemType of the item where the profile will be stored
     * @param profile      the XML profile which will be stored
     * @param callback     if set and responseType is jason the result will be returned
     *                     via this javascript callback function (optional)
     * @return a response object containing information about the success of the operation
     */
    @GET
    @Path("/store")
    public Response storeProfile(@PathParam("responseType") String responseType,
                                 @QueryParam("apikey") String apiKey,
                                 @QueryParam("tenantid") String tenantID,
                                 @QueryParam("itemid") String itemID,
                                 @DefaultValue("ITEM") @QueryParam("itemtype") String itemType,
                                 @QueryParam("profile") String profile,
                                 @QueryParam("callback") String callback) {

        Monitor mon = MonitorFactory.start(JAMON_PROFILE_STORE);

        List<Message> messages = new ArrayList<Message>();
        Object responseObject = null;

        try {
            if (checkParameters(apiKey, tenantID, itemID, itemType, messages)) {
                Integer coreTenantID = operatorDAO.getTenantId(apiKey, tenantID);
                if (coreTenantID == null)
                    messages.add(MSG.TENANT_WRONG_TENANT_APIKEY);
                else {
                    if (profileService.storeProfile(coreTenantID, itemID, itemType, profile))
                        messages.add(MSG.PROFILE_SAVED);
                    else
                        messages.add(MSG.PROFILE_NOT_SAVED);
                }
            }
        } catch (RuntimeException runtimeException) {
            messages.add(MSG.PROFILE_NOT_SAVED);
        }

        Response response = formatResponse(responseObject, messages,
                WS.PROFILE_STORE, responseType, callback);
        mon.stop();
        return response;
    }

    /**
     * This method deletes the profile of the item defined by the tenantID,
     * itemID and the itemTypeID
     *
     * @param responseType defines the media type of the result
     * @param apiKey       the apiKey which admits access to the API
     * @param tenantID     the tenantID of the item whose profile will be deleted
     * @param itemID       the itemID if the item whose profile will be deleted
     * @param itemType     the itemType of the item whose profile will be deleted
     * @param callback     if set and responseType is jason the result will be returned
     *                     via this javascript callback function (optional)
     * @return a response object containing information about the success of the operation
     */
    @GET
    @Path("/delete")
    public Response deleteProfile(@PathParam("responseType") String responseType,
                                  @QueryParam("apikey") String apiKey,
                                  @QueryParam("tenantid") String tenantID,
                                  @QueryParam("itemid") String itemID,
                                  @DefaultValue("ITEM") @QueryParam("itemtype") String itemType,
                                  @QueryParam("callback") String callback) {

        Monitor mon = MonitorFactory.start(JAMON_PROFILE_DELETE);

        List<Message> messages = new ArrayList<Message>();
        Object responseObject = null;

        try {
            if (checkParameters(apiKey, tenantID, itemID, itemType, messages)) {
                Integer coreTenantID = operatorDAO.getTenantId(apiKey, tenantID);
                if (coreTenantID == null)
                    messages.add(MSG.TENANT_WRONG_TENANT_APIKEY);
                else {
                    if (profileService.deleteProfile(coreTenantID, itemID, itemType))
                        messages.add(MSG.PROFILE_DELETED);
                    else
                        messages.add(MSG.PROFILE_NOT_DELETED);
                }
            }
        } catch (RuntimeException runtimeException) {
            messages.add(MSG.PROFILE_NOT_DELETED);
        }

        Response response = formatResponse(responseObject, messages,
                WS.PROFILE_DELETE, responseType, callback);
        mon.stop();
        return response;
    }

    /**
     * This method returns the profile of the item defined by the tenantID,
     * itemID and the itemTypeID
     *
     * @param responseType defines the media type of the result
     * @param apiKey       the apiKey which admits access to the API
     * @param tenantID     the tenantID of the item whose profile will be returned
     * @param itemID       the itemID if the item whose profile will be returned
     * @param itemType     the itemType of the item whose profile will be returned
     * @param callback     if set and responseType is jason the result will be returned
     *                     via this javascript callback function (optional)
     * @return a response object containing the wanted profile
     */
    @GET
    @Path("/load")
    public Response loadProfile(@PathParam("responseType") String responseType,
                                @QueryParam("apikey") String apiKey,
                                @QueryParam("tenantid") String tenantID,
                                @QueryParam("itemid") String itemID,
                                @DefaultValue("ITEM") @QueryParam("itemtype") String itemType,
                                @QueryParam("callback") String callback) {

        Monitor mon = MonitorFactory.start(JAMON_PROFILE_LOAD);

        List<Message> messages = new ArrayList<Message>();
        Object responseObject = null;

        try {
            if (checkParameters(apiKey, tenantID, itemID, itemType, messages)) {
                Integer coreTenantID = operatorDAO.getTenantId(apiKey, tenantID);
                if (coreTenantID == null)
                    messages.add(MSG.TENANT_WRONG_TENANT_APIKEY);
                else {
                    String profile = profileService.getProfile(coreTenantID, itemID, itemType);
                    if (profile != null)
                        responseObject = new ResponseProfile("/profile/load", tenantID, itemID, itemType, profile);
                    else
                        messages.add(MSG.PROFILE_NOT_LOADED);
                }
            }
        } catch (RuntimeException runtimeException) {
            messages.add(MSG.PROFILE_NOT_LOADED);
        }

        Response response = formatResponse(responseObject, messages,
                WS.PROFILE_LOAD, responseType, callback);
        mon.stop();
        return response;
    }

    /**
     * This method stores a value into a specific field of the profile which belongs to the item
     * defined by the tenantID, itemID and the itemTypeID. The field can be addressed
     * by a XPath expression
     *
     * @param responseType defines the media type of the result
     * @param apiKey       the apiKey which admits access to the API
     * @param tenantID     the tenantID of the addressed item
     * @param itemID       the itemID if the addressed item
     * @param itemType     the itemType of the addressed item
     * @param field        an XPath expression pointing to the field
     *                     where the value will be saved
     * @param value        the value which will be saved in the field
     * @param callback     if set and responseType is jason the result will be returned
     *                     via this javascript callback function (optional)
     * @return a response object containing information about the success of the operation
     */
    @GET
    @Path("/field/store")
    public Response storeField(@PathParam("responseType") String responseType,
                               @QueryParam("apikey") String apiKey,
                               @QueryParam("tenantid") String tenantID,
                               @QueryParam("itemid") String itemID,
                               @DefaultValue("ITEM") @QueryParam("itemtype") String itemType,
                               @QueryParam("field") String field,
                               @QueryParam("value") String value,
                               @QueryParam("callback") String callback) {

        Monitor mon = MonitorFactory.start(JAMON_PROFILE_FIELD_STORE);

        List<Message> messages = new ArrayList<Message>();
        Object responseObject = null;

        try {
            if (checkParameters(apiKey, tenantID, itemID, itemType, messages)) {
                Integer coreTenantID = operatorDAO.getTenantId(apiKey, tenantID);
                if (coreTenantID == null)
                    messages.add(MSG.TENANT_WRONG_TENANT_APIKEY);
                else {
                    if (profileService.insertSimpleDimension(
                            coreTenantID, itemID, itemType,
                            field, value))
                        messages.add(MSG.PROFILE_FIELD_SAVED);
                    else
                        messages.add(MSG.PROFILE_FIELD_NOT_SAVED);
                }
            }
        } catch (RuntimeException runtimeException) {
            messages.add(MSG.PROFILE_FIELD_NOT_SAVED);
        }

        Response response = formatResponse(responseObject, messages,
                WS.PROFILE_FIELD_STORE, responseType, callback);
        mon.stop();
        return response;
    }

    /**
     * This method deletes a specific field of the profile which belongs to the item
     * defined by the tenantID, itemID and the itemTypeID. The field can be addressed
     * by a XPath expression
     *
     * @param responseType defines the media type of the result
     * @param apiKey       the apiKey which admits access to the API
     * @param tenantID     the tenantID of the addressed item
     * @param itemID       the itemID if the addressed item
     * @param itemType     the itemType of the addressed item
     * @param field        an XPath expression pointing to the field
     *                     which will be deleted
     * @param callback     if set and responseType is jason the result will be returned
     *                     via this javascript callback function (optional)
     * @return a response object containing information about the success of the operation
     */
    @GET
    @Path("/field/delete")
    public Response deleteField(@PathParam("responseType") String responseType,
                                @QueryParam("apikey") String apiKey,
                                @QueryParam("tenantid") String tenantID,
                                @QueryParam("itemid") String itemID,
                                @DefaultValue("ITEM") @QueryParam("itemtype") String itemType,
                                @QueryParam("field") String field,
                                @QueryParam("callback") String callback) {

        Monitor mon = MonitorFactory.start(JAMON_PROFILE_DELETE);

        List<Message> messages = new ArrayList<Message>();
        Object responseObject = null;

        try {
            if (checkParameters(apiKey, tenantID, itemID, itemType, messages)) {
                Integer coreTenantID = operatorDAO.getTenantId(apiKey, tenantID);
                if (coreTenantID == null)
                    messages.add(MSG.TENANT_WRONG_TENANT_APIKEY);
                else {
                    if (profileService.deleteValue(coreTenantID, itemID, itemType, field))
                        messages.add(MSG.PROFILE_FIELD_DELETED);
                    else
                        messages.add(MSG.PROFILE_FIELD_NOT_DELETED);
                }
            }
        } catch (RuntimeException runtimeException) {
            messages.add(MSG.PROFILE_FIELD_NOT_DELETED);
        }

        Response response = formatResponse(responseObject, messages,
                WS.PROFILE_FIELD_DELETE, responseType, callback);
        mon.stop();
        return response;
    }

    /**
     * This method loads the values from a specific field of the profile which belongs to the item
     * defined by the tenantID, itemID and the itemTypeID. The field can be addressed
     * by a XPath expression
     *
     * @param responseType defines the media type of the result
     * @param apiKey       the apiKey which admits access to the API
     * @param tenantID     the tenantID of the addressed item
     * @param itemID       the itemID if the addressed item
     * @param itemType     the itemType of the addressed item
     * @param field        an XPath expression pointing to the field(s)
     *                     whose value will be returned
     * @param callback     if set and responseType is jason the result will be returned
     *                     via this javascript callback function (optional)
     * @return a response object containing the values of the field.
     * @see ResponseProfileField
     */
    @GET
    @Path("/field/load")
    public Response loadField(@PathParam("responseType") String responseType,
                              @QueryParam("apikey") String apiKey,
                              @QueryParam("tenantid") String tenantID,
                              @QueryParam("itemid") String itemID,
                              @DefaultValue("ITEM") @QueryParam("itemtype") String itemType,
                              @QueryParam("field") String field,
                              @QueryParam("callback") String callback) {

        Monitor mon = MonitorFactory.start(JAMON_PROFILE_FIELD_LOAD);

        List<Message> messages = new ArrayList<Message>();
        Object responseObject = null;

        try {
            if (checkParameters(apiKey, tenantID, itemID, itemType, messages)) {
                Integer coreTenantID = operatorDAO.getTenantId(apiKey, tenantID);
                if (coreTenantID == null)
                    messages.add(MSG.TENANT_WRONG_TENANT_APIKEY);
                else {
                    Set<String> values = profileService.getMultiDimensionValue(coreTenantID, itemID, itemType, field);
                    if (values != null)
                        responseObject = new ResponseProfileField("/profile/field/load", tenantID, itemID, itemType, values);
                    else
                        messages.add(MSG.PROFILE_FIELD_NOT_LOADED);
                }
            }
        } catch (RuntimeException runtimeException) {
            messages.add(MSG.PROFILE_FIELD_NOT_LOADED);
        }

        Response response = formatResponse(responseObject, messages,
                WS.PROFILE_FIELD_LOAD, responseType, callback);
        mon.stop();
        return response;
    }


    /**
     * This method takes an object and creates a <code>Response</code> object
     * out of it which will be returned. If <code>messages</code> contains error
     * messages they will be send back instead.
     * The format of the <code>Response</code>
     * depends on the <code>responseType</code>.
     * Supported types are <code>application/xml</code> and <code>application/json</code>
     *
     * @param respondData  an object which will be returned as a
     *                     <code>Response</code> object
     * @param messages     a list of <code>Message</code> objects which contain
     *                     error messages of the API request
     * @param responseType defines the format of the <code>Response</code> object
     * @param callback     if set and responseType is jason the result will be returned
     *                     via this javascript callback function (optional)
     * @return a <code>Response</code> object containing the <code>responseData</code>
     *         in the format defined with <code>responseType</code>
     */
    private Response formatResponse(Object respondData,
                                    List<Message> messages,
                                    String serviceName,
                                    String responseType,
                                    String callback) {

        //handle error messages if existing
        if (messages.size() > 0) {
            if ((WS.RESPONSE_TYPE_PATH_JSON.equals(responseType)))
                throw new EasyRecException(messages, serviceName, WS.RESPONSE_TYPE_JSON, callback);
            else
                throw new EasyRecException(messages, serviceName);
        }

        //convert respondData to Respond object
        if (WS.RESPONSE_TYPE_PATH_JSON.equals(responseType)) {
            if (callback != null) {
                return Response.ok(new JSONWithPadding(respondData, callback),
                        WS.RESPONSE_TYPE_JSCRIPT).build();
            } else {
                return Response.ok(respondData, WS.RESPONSE_TYPE_JSON).build();
            }
        } else if (WS.RESPONSE_TYPE_PATH_XML.equals(responseType)) {
            return Response.ok(respondData, WS.RESPONSE_TYPE_XML).build();
        } else {
            return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build();
        }
    }

    /**
     * This method checks if the <code>apiKey, tenantID, itemID</code> and <code>itemType</code>
     * is not null or an empty string. If this is not the case the corresponding error message is
     * added to the <code>messages</code> list.
     *
     * @param apiKey   the apiKey which will be checked
     * @param tenantID the tenantID which will be checked
     * @param itemID   the itemID which will be checked
     * @param itemType the itemType which will be checked
     * @param messages a <code>List&lt;Message&gt;</code> where the error messages are appended
     * @return returns <code>true</code> if all parameters are positively checked and
     *         <code>false</code> otherwise
     */
    private boolean checkParameters(String apiKey, String tenantID,
                                    String itemID, String itemType, List<Message> messages) {
        boolean result = true;
        if (apiKey == null || apiKey.equals("")) {
            messages.add(MSG.PROFILE_NO_API_KEY);
            result = false;
        }
        if (tenantID == null || tenantID.equals("")) {
            messages.add(MSG.PROFILE_NO_TENANT_ID);
            result = false;
        }
        if (itemID == null || itemID.equals("")) {
            messages.add(MSG.ITEM_NO_ID);
            result = false;
        }
        if (itemType == null || itemType.equals("")) {
            messages.add(MSG.PROFILE_NO_ITEM_TYPE);
            result = false;
        }
        return result;
    }
}
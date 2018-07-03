/*
 * Copyright (c) 2002-2018, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.easyrulesbot.modules.ldap.service;

import fr.paris.lutece.plugins.easyrulesbot.service.response.exceptions.ResponseNotUnderstoodException;
import fr.paris.lutece.plugins.easyrulesbot.service.response.exceptions.ResponseProcessingException;
import fr.paris.lutece.plugins.easyrulesbot.service.response.processors.AbstractProcessor;
import fr.paris.lutece.plugins.easyrulesbot.service.response.processors.ResponseProcessor;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.template.AppTemplateService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.portal.web.l10n.LocaleService;
import fr.paris.lutece.util.html.HtmlTemplate;
import fr.paris.lutece.util.ldap.LdapUtil;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.naming.CommunicationException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.apache.commons.lang.StringUtils;

/**
 * SearchValueProcessor
 */
public class SearchValueProcessor extends AbstractProcessor implements ResponseProcessor
{
    //ldap
    private static final String PROPERTY_INITIAL_CONTEXT_PROVIDER = "easyrulesbot-ldap.ldap.initialContextProvider";
    private static final String PROPERTY_PROVIDER_URL = "easyrulesbot-ldap.ldap.connectionUrl";
    private static final String PROPERTY_BIND_DN = "easyrulesbot-ldap.ldap.connectionName";
    private static final String PROPERTY_BIND_PASSWORD = "easyrulesbot-ldap.ldap.connectionPassword";
    private static final String PROPERTY_USER_DN_SEARCH_BASE = "easyrulesbot-ldap.ldap.personBase";
    private static final String PROPERTY_USER_SUBTREE = "easyrulesbot-ldap.ldap.personSubtree";
    private static final String PROPERTY_FILTER_PARAMETER_PREFIX = "easyrulesbot-ldap.ldap.filter.parameter";
    private static final String PROPERTY_DN_ATTRIBUTE_PREFIX = "easyrulesbot-ldap.ldap.dn.attributeName";

    private static final String TEMPLATE_LDAP_FILE = "skin/plugins/easyrulesbot/modules/ldap/ldap.html";
    private static final String MARK_PERSONS_LIST = "persons_list";
    private static final String MARK_CRITERIA_LIST = "criteria_list";

    private static String _strSearchField;
    private static String _strLdapSearch;
    private static String _strDefaultSearchField = "default";
    private static String _strShowDirectory;

    private static String _strInitialContextProvider = AppPropertiesService.getProperty( PROPERTY_INITIAL_CONTEXT_PROVIDER );
    private static String _strProviderUrl = AppPropertiesService.getProperty( PROPERTY_PROVIDER_URL );
    private static String _strBindDn = AppPropertiesService.getProperty( PROPERTY_BIND_DN );
    private static String _strBindPassword = AppPropertiesService.getProperty( PROPERTY_BIND_PASSWORD );
    private static String _strPersonDnSearchBase = AppPropertiesService.getProperty( PROPERTY_USER_DN_SEARCH_BASE );
    private static int _nPersonDnSearchScope = AppPropertiesService.getProperty( PROPERTY_USER_SUBTREE ).equalsIgnoreCase( "true" ) ? SearchControls.SUBTREE_SCOPE : SearchControls.ONELEVEL_SCOPE ;

    private String _strInvalidResponseMessage;
    private String _strInvalidResponseMessageI18nKey;


    /**
     * {@inheritDoc }
     */
    @Override
    public String processResponse( String strResponse, Locale locale, Map mapData ) throws ResponseProcessingException
    {
        Map<String, String> map = (Map<String, String>) mapData;

        if ( strResponse != null && !strResponse.isEmpty(  ) )
        {
            String strCriteriaName = !StringUtils.isEmpty( map.get( _strSearchField ) ) ? map.get( _strSearchField ) : _strDefaultSearchField;
            String strParameters = !StringUtils.isEmpty( map.get( _strLdapSearch ) ) ? map.get( _strLdapSearch ) : "";
            String strDelimiter = !StringUtils.isEmpty( strParameters ) ? "," : "";

            strParameters += strDelimiter + strCriteriaName + ":" + strResponse;  
            mapData.put( _strLdapSearch, strParameters ); 

            String strDirectory = buildDirectory( mapData );
            mapData.put( _strShowDirectory, strDirectory );

            return strResponse;
        }
        
        throw new ResponseNotUnderstoodException( getInvalidResponse( locale ) );
    }

    /**
     * Set the Invalid Response Message
     * 
     * @param strMessage
     *            The message
     */
    public void setInvalidResponseMessage( String strMessage )
    {
        _strInvalidResponseMessage = strMessage;
    }

    /**
     * Set the Invalid Response Message
     * 
     * @param strMessage
     *            The message
     */
    public void setInvalidResponseMessageI18nKey( String strMessage )
    {
        _strInvalidResponseMessageI18nKey = strMessage;
    }

    /**
     * Set the search field key
     * 
     * @param strSearchField
     *            The search field key
     */
    public void setSearchField( String strSearchField )
    {
        _strSearchField = strSearchField;
    }

    /**
     * Set the ldap search key
     * 
     * @param strLdapSearch
     *            The ldap search key
     */
    public void setLdapSearch( String strLdapSearch )
    {
        _strLdapSearch = strLdapSearch;
    }

    /**
     * Set the show directory key
     * 
     * @param strShowDirectory
     *            The show directory key
     */
    public void setShowDirectory( String strShowDirectory )
    {
        _strShowDirectory = strShowDirectory;
    }

    /**
     * Returns invalid response message
     * 
     * @param locale
     *            The locale
     * @return The message
     */
    private String getInvalidResponse( Locale locale )
    {
        String strResponse;

        if ( _strInvalidResponseMessageI18nKey != null )
        {
            strResponse = I18nService.getLocalizedString( _strInvalidResponseMessageI18nKey, locale );
        }
        else
        {
            strResponse = _strInvalidResponseMessage;
        }

        return strResponse;
    }

    /**
     * Build the directory table
     * 
     * @param mapData
     *            The map of data
     * @return The directory table
     */
    private String buildDirectory( Map mapData )
    {
        Map<String, String> mapPersonSearchCriteria = getMapParameters( mapData );
        
        Map<String, Object> model = new HashMap<String, Object>( );
        model.put( MARK_PERSONS_LIST, getPersonList( mapPersonSearchCriteria ) );
        model.put( MARK_CRITERIA_LIST, mapPersonSearchCriteria );

        HtmlTemplate template = AppTemplateService.getTemplate( TEMPLATE_LDAP_FILE, LocaleService.getDefault(  ), model );

        return template.getHtml( );
    }

    /**
     * Get the list of persons from ldap
     * 
     * @param mapPersonSearchCriteria
     *            The map of search criteria
     * @return The list of persons
     */
    private Collection getPersonList( Map<String, String> mapPersonSearchCriteria )
    {
        ArrayList<Map> personList = new ArrayList<Map>(  );
        SearchResult sr = null;

        DirContext context = null;
        
        String strPersonSearchFilter = getParameters( mapPersonSearchCriteria );

        try
        {
            SearchControls scPersonSearchControls = new SearchControls(  );
            scPersonSearchControls.setSearchScope( _nPersonDnSearchScope );
            scPersonSearchControls.setReturningObjFlag( true );
            scPersonSearchControls.setCountLimit( 0 );

            context = LdapUtil.getContext( _strInitialContextProvider, _strProviderUrl, _strBindDn,
                    _strBindPassword );

            NamingEnumeration personResults = LdapUtil.searchUsers( context, strPersonSearchFilter,
                    _strPersonDnSearchBase, "", scPersonSearchControls );

            AppLogService.info( this.getClass(  ).toString(  ) + " : Search persons with searchFilter" + 
                    strPersonSearchFilter );

            while ( ( personResults != null ) && personResults.hasMore(  ) )
            {
                sr = (SearchResult) personResults.next(  );

                Attributes attributes = sr.getAttributes(  );

                HashMap<String, String> attributeMap = new HashMap<String, String>(  );

                for ( String strAttributeKey : AppPropertiesService.getKeys( PROPERTY_DN_ATTRIBUTE_PREFIX ) )
                {
                    String strAttributeName = AppPropertiesService.getProperty( strAttributeKey );
                    Attribute attribute = attributes.get( strAttributeName );
                    String strAttributeValue = "";

                    if ( attribute != null )
                    {
                        strAttributeValue = attribute.get(  ).toString(  );
                    }

                    attributeMap.put( strAttributeKey.replace( PROPERTY_DN_ATTRIBUTE_PREFIX + ".", ""), strAttributeValue );
                }

                personList.add( attributeMap );
            }

            return personList;
        }
        catch ( CommunicationException e )
        {
            AppLogService.error( "Error while searching for persons with search filter : " +
                getDebugInfo( strPersonSearchFilter ), e );

            return personList;
        }
        catch ( NamingException e )
        {
            AppLogService.error( "Error while searching for persons " );

            return personList;
        }
        finally
        {
            try
            {
                LdapUtil.freeContext( context );
            }
            catch ( NamingException naming )
            {
                //todo
            }
        }
    }

    /**
     * Gets the criteria parameters map
     * 
     * @param mapData
     *            The data provided by the bot
     * @return The criteria parameters list
     */
    private static Map<String, String> getMapParameters( Map mapData )
    {
        Map<String, String> map = (Map<String, String>) mapData;
        Map<String, String> mapCriteria = new HashMap<String, String>();

        String strCriteria = map.get( _strLdapSearch );

        if ( strCriteria != null )
        {
            String [ ] tabCriteria = strCriteria.split( "," );

            for ( String strCriterion : tabCriteria )
            {
                int x = strCriterion.indexOf(":");
                if ( x > -1 )
                {
                    String strKey = strCriterion.substring(0, x);
                    String strValue = strCriterion.substring(x + 1);
                    mapCriteria.put(strKey, strValue);
                }
            }
        }

        return mapCriteria;
    }

    /**
     * Gets the criteria parameters string
     * 
     * @param mapSearchCriteria
     *            The map of search criteria
     * @return The criteria parameters list
     */
    private static String getParameters( Map<String, String> mapSearchCriteria )
    {
        String strParameters = "";

        for (Map.Entry<String, String> entry : mapSearchCriteria.entrySet())
        {
            String strCriteriaName = entry.getKey();
            String strCriteriaKey = AppPropertiesService.getProperty( PROPERTY_FILTER_PARAMETER_PREFIX + "." + strCriteriaName);
            String[] strCriteriaValue = entry.getValue( ).split( " " );
            String strParameter = "";

            for (String strCriteriaToken : strCriteriaValue)
            {
                if( !StringUtils.isEmpty( strCriteriaKey ) && !StringUtils.isEmpty( strCriteriaToken ) )
                {
                    strParameter += MessageFormat.format( strCriteriaKey, strCriteriaToken);
                }
            }
            strParameters += strParameter;
        }

        return ( strParameters.isEmpty( ) ) ? "" : "(&" + strParameters + ")";
    }

    /**
     * Return info for debugging
     * 
     * @param PersonSearchFilter
     *            The person search filter
     * @return Info for debugging
     */
    private String getDebugInfo( String strPersonSearchFilter )
    {
        StringBuffer sb = new StringBuffer(  );
        sb.append( "personBase : " );
        sb.append( _strPersonDnSearchBase );
        sb.append( "\npersonSearch : " );
        sb.append( strPersonSearchFilter );

        return sb.toString(  );
    }
}

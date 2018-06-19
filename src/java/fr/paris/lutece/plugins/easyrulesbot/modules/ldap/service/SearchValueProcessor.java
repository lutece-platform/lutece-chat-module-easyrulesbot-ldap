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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
    private static final String PROPERTY_DN_ATTRIBUTE_PREFIX = "easyrulesbot-ldap.ldap.dn.attributeName";

    private static final String TEMPLATE_LDAP_FILE = "skin/plugins/easyrulesbot/modules/ldap/ldap.html";
    private static final String MARK_PERSONS_LIST = "persons_list";

    private static String _strSearchField;
    private static String _strLdapSearch;
    private static String _strShowDirectory;
    private String _strInvalidResponseMessage;
    private String _strInvalidResponseMessageI18nKey;


    /**
     * {@inheritDoc }
     */
    @Override
    public String processResponse( String strResponse, Locale locale, Map mapData ) throws ResponseProcessingException
    {
        Map<String, String> map = (Map<String, String>) mapData;
        String strParameters = ( map.get( _strLdapSearch ) != null ) ? map.get( _strLdapSearch ) : "" ;

        for ( String strDataKey : map.keySet( ) )
        {
            if ( strDataKey.equals( _strSearchField ) && strResponse != null && !strResponse.isEmpty(  ) )
            {
                String strCriteriaName = map.get( strDataKey );
                String strCriteriaKey = AppPropertiesService.getProperty( PROPERTY_DN_ATTRIBUTE_PREFIX + "." + strCriteriaName);
                strParameters += "(" + strCriteriaKey + "=" + strResponse + "*)";
                mapData.put( _strLdapSearch, strParameters );

                String strDirectory = buildDirectory( mapData );
                mapData.put( _strShowDirectory, strDirectory );

                return strResponse;
            }
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
        Map<String, Object> model = new HashMap<String, Object>( );
        model.put( MARK_PERSONS_LIST, getPersonList( mapData ) );

        HtmlTemplate template = AppTemplateService.getTemplate( TEMPLATE_LDAP_FILE, LocaleService.getDefault(  ), model );

        return template.getHtml( );
    }

    /**
     * Get the list of persons from ldap
     * 
     * @param mapData
     *            The map of data
     * @return The list of persons
     */
    private Collection getPersonList( Map mapData )
    {
        ArrayList<Map> personList = new ArrayList<Map>(  );
        SearchResult sr = null;

        DirContext context = null;

        String strPersonSearchFilter = getParameters( mapData );

        try
        {
            SearchControls scPersonSearchControls = new SearchControls(  );
            scPersonSearchControls.setSearchScope( getPersonDnSearchScope(  ) );
            scPersonSearchControls.setReturningObjFlag( true );
            scPersonSearchControls.setCountLimit( 0 );

            context = LdapUtil.getContext( getInitialContextProvider(  ), getProviderUrl(  ), getBindDn(  ),
                    getBindPassword(  ) );

            NamingEnumeration personResults = LdapUtil.searchUsers( context, strPersonSearchFilter,
                    getPersonDnSearchBase(  ), "", scPersonSearchControls );

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
     * Gets the criteria parameters list
     * 
     * @param mapData
     *            The data provided by the bot
     * @return The criteria parameters list
     */
    private static String getParameters( Map mapData )
    {
        Map<String, String> map = (Map<String, String>) mapData;
        for ( String strDataKey : map.keySet( ) )
        {
            if ( strDataKey.equals( _strLdapSearch ) )
            {
                return "(&" + mapData.get( strDataKey ) + ")";
            }
        }

        return "";
    }

    /**
     * Get the initial context provider from the properties
     * 
     * @return The initial context provider
     */
    private String getInitialContextProvider(  )
    {
        return AppPropertiesService.getProperty( PROPERTY_INITIAL_CONTEXT_PROVIDER );
    }

    /**
     * Get the provider url from the properties
     * 
     * @return The provider url
     */
    private String getProviderUrl(  )
    {
        return AppPropertiesService.getProperty( PROPERTY_PROVIDER_URL );
    }

    /**
     * Get the base person dn from the properties
     * 
     * @return The base person dn
     */
    private String getPersonDnSearchBase(  )
    {
        return AppPropertiesService.getProperty( PROPERTY_USER_DN_SEARCH_BASE );
    }

    /**
     * Get the person dn search scope
     * 
     * @return The person dn search scope
     */
    private int getPersonDnSearchScope(  )
    {
        String strSearchScope = AppPropertiesService.getProperty( PROPERTY_USER_SUBTREE );

        if ( strSearchScope.equalsIgnoreCase( "true" ) )
        {
            return SearchControls.SUBTREE_SCOPE;
        }

        return SearchControls.ONELEVEL_SCOPE;
    }

    /**
     * Get the bind dn
     * 
     * @return The bind dn
     */
    private String getBindDn(  )
    {
        return AppPropertiesService.getProperty( PROPERTY_BIND_DN );
    }

    /**
     * Get the bing password
     * 
     * @return The bing password
     */
    private String getBindPassword(  )
    {
        return AppPropertiesService.getProperty( PROPERTY_BIND_PASSWORD );
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
        sb.append( getPersonDnSearchBase(  ) );
        sb.append( "\npersonSearch : " );
        sb.append( strPersonSearchFilter );

        return sb.toString(  );
    }
}

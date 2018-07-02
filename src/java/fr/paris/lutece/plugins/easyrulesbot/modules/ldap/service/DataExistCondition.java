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

import fr.paris.lutece.plugins.easyrulesbot.service.bot.rules.conditions.AbstractCondition;
import fr.paris.lutece.plugins.easyrulesbot.service.bot.rules.conditions.Condition;
import java.util.List;
import java.util.Map;

/**
 * DataMissingCondition
 */
public class DataExistCondition  extends AbstractCondition implements Condition
{
    /**
     * {@inheritDoc }
     */
    @Override
    public boolean evaluate( Map<String, String> mapData, String strRuleDataKey )
    {
        List<String> listParameters = getParameters();
        
        for( String strDataKey : listParameters )
        {
            if( !mapData.containsKey( strDataKey ))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String toString()
    {
        StringBuilder sbOutput = new StringBuilder( "Condition DataExist : following datakeys should be defined :");
        if(  getParameters() != null )
        {
            for( String strDataKey : getParameters() )
            {
                sbOutput.append( " " ).append( strDataKey );
            }
        }
        return sbOutput.toString(); 
    }
    
    
}

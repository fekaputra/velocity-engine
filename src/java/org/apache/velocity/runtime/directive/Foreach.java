package org.apache.velocity.runtime.directive;

/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

import java.io.Writer;
import java.io.IOException;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import org.apache.velocity.Context;
import org.apache.velocity.runtime.Runtime;
import org.apache.velocity.util.ClassUtils;
import org.apache.velocity.util.ArrayIterator;

import org.apache.velocity.runtime.parser.Node;
import org.apache.velocity.runtime.parser.Token;

/**
 * Foreach directive used for moving through arrays,
 * or objects that provide an Iterator.
 */
public class Foreach extends Directive
{
    private final static int ARRAY = 1;
    private final static int ITERATOR = 2;
    
    private final static String COUNTER_IDENTIFIER =
        Runtime.getString(Runtime.COUNTER_NAME);
    
    private final static int COUNTER_INITIAL_VALUE =
        new Integer(Runtime.getString(Runtime.COUNTER_INITIAL_VALUE)).intValue();

    private String elementKey;
    private Object listObject;
    private Object tmp;
    private int iterator;

    public String getName() 
    { 
        return "foreach"; 
    }
    
    public int getType() 
    { 
        return BLOCK; 
    }

    public void init(Context context, Node node) throws Exception
    {
        Object sampleElement = null;
        elementKey = node.jjtGetChild(0).getFirstToken().image.substring(1);
        
        // This is a refence node and it needs to
        // be inititialized.
        
        node.jjtGetChild(2).init(context, null);
        listObject = node.jjtGetChild(2).value(context);
        
        // Figure out what type of object the list
        // element is so that we don't have to do it
        // everytime the node is traversed.
        
        if (listObject instanceof Object[])
        {
            node.setInfo(ARRAY);
            sampleElement = ((Object[]) listObject)[0];
        }            
        else if (ClassUtils.implementsMethod(listObject, "iterator"))
        {
            node.setInfo(ITERATOR);
            sampleElement = ((Collection) listObject).iterator().next();
        }            
        
        // This is a little trick so that we can initialize
        // all the blocks in the foreach  properly given
        // that there are references that refer to the
        // elementKey name.
        if (sampleElement != null)
        {
            context.put(elementKey, sampleElement);
            super.init(context, node);
            context.remove(elementKey);
        }            
    }

    public void render(Context context, Writer writer, Node node)
        throws IOException
    {
        Iterator i;
        listObject = node.jjtGetChild(2).value(context);

        if (node.getInfo() == ARRAY)
            i = new ArrayIterator((Object[]) listObject);
        else            
            i = ((Collection) listObject).iterator();
        
        iterator = COUNTER_INITIAL_VALUE;
        while (i.hasNext())
        {
            context.put(COUNTER_IDENTIFIER, new Integer(iterator));
            context.put(elementKey,i.next());
            node.jjtGetChild(3).render(context, writer);
            iterator++;
        }
        context.remove(COUNTER_IDENTIFIER);
        context.remove(elementKey);
    }
}

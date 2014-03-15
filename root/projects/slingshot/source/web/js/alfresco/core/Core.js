/**
 * Copyright (C) 2005-2013 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * This should be mixed into all Alfresco widgets as it provides the essential functions that they will 
 * undoubtedly required, e.g. logging, publication/subscription handling, i18n message handling, etc. 
 * 
 * @module alfresco/core/Core
 * @author Dave Draper
 */
define(["dojo/_base/declare",
        "alfresco/core/CoreData",
        "alfresco/core/PubSubLog",
        "service/constants/Default",
        "dojo/topic",
        "dojo/_base/array",
        "dojo/_base/lang",
        "dojox/uuid/generateRandomUuid",
        "dojox/html/entities"], 
        function(declare, CoreData, PubSubLog, AlfConstants, pubSub, array, lang, uuid, htmlEntities) {
   
   return declare(null, {
      
      /**
       * This has been added purely to prevent any object that inherits from this mixin from being 
       * iterated over in the pub/sub log. It aims to prevent infinite loops (although there is protection
       * for this in the [SubscriptionLog]{@link module:alfresco/testing/SubscriptionLog}) module). It should
       * also ensure that only useful information is displayed in the log.
       * 
       * @instance
       * @type {boolean}
       * @default true
       */
      excludeFromPubSubLog: true,

      /**
       * Creates and returns a new UUID (universally unique identifier). The UUID is generated using the
       * dojox/uuid/generateRandomUuid module
       * 
       * @instance
       * @returns {string} A new UUID
       */
      generateUuid: function alfresco_core_Core__generateUuid() {
         return uuid();
      },
      
      /**
       * This function is based on the version that can be found in alfresco.js. It searches through all of
       * the available scopes for the widget and for all of the widgets inherited from.
       * 
       * @instance
       * @param {string} p_messageId The id of the message to be displayed.
       * @returns {string} A localized form of the supplied message
       */
      message: function alfresco_core_Core__message(p_messageId) {

         if (typeof p_messageId != "string")
         {
            throw new Error("Missing or invalid argument: messageId");
         }

         var msg = p_messageId;
         
         // Check the global message bundle for the message id (this will get overridden if a more specific
         // property is available)...
         if (typeof Alfresco.messages.global === "object")
         {
            var globalMsg = Alfresco.messages.global[p_messageId];
            if (typeof globalMsg == "string")
            {
               msg = globalMsg;
            }
         }
         
         // Overwrite with page scope...
         if (typeof Alfresco.messages.pageScope === "object")
         {
            var pageScopeMsg = Alfresco.messages.pageScope[p_messageId];
            if (typeof pageScopeMsg == "string")
            {
               msg = pageScopeMsg;
            }
         }
         
         // Overwrite page scope with default scope...
         if (typeof Alfresco.messages.scope[Alfresco.messages.defaultScope] === "object")
         {
            var scopeMsg = Alfresco.messages.scope[Alfresco.messages.defaultScope][p_messageId];
            if (typeof scopeMsg == "string")
            {
               msg = scopeMsg;
            }
         }
         
         // Work through the base classes and use their i18nScope property (if available) as a scope to 
         // check. This allows a widget to check its class hierarchy for message scopes.
         array.forEach(this.constructor._meta.parents, function(entry, i) {
            
            // PLEASE NOTE: Use of the constructor _meta property is used at risk. It is the recognised
            //              way of accessing parent classes (for example it is used in the .isInstanceOf()
            //              function but there is a warning that it is not part of an API that can be relied
            //              upon to never change. Should message handling fail, then this might be an area
            //              to investigate.
            if (entry._meta && entry._meta.hidden && entry._meta.hidden.i18nScope && Alfresco.messages.scope[entry._meta.hidden.i18nScope])
            {
               var scopeMsg = Alfresco.messages.scope[entry._meta.hidden.i18nScope][p_messageId];
               if (typeof scopeMsg == "string")
               {
                  msg = scopeMsg;
               }
            }
         });
         
         // Set the main scope for the calling class...
         // This will either be the i18nScope or the default message scope if i18nScope is not defined
         var messageScope;
         if (typeof this.i18nScope != "undefined" && typeof Alfresco.messages.scope[this.i18nScope] === "object")
         {
            var scopeMsg = Alfresco.messages.scope[this.i18nScope][p_messageId];
            if (typeof scopeMsg == "string")
            {
               msg = scopeMsg;
            }
         }
         
         // Search/replace tokens
         var tokens = [];
         if ((arguments.length == 2) && (typeof arguments[1] == "object"))
         {
            tokens = arguments[1];
         }
         else
         {
            tokens = Array.prototype.slice.call(arguments).slice(2);
         }
         
         // Emulate server-side I18NUtils implementation
         if (tokens instanceof Array && tokens.length > 0)
         {
            msg = msg.replace(/''/g, "'");
         }
         
         // TODO: Need to check this works with old Share strings...
         msg = lang.replace(msg, tokens);
         return msg;
      },
      
      /**
       * Use this function to ensure that all text added to the HTML page is encoded to prevent XSS style
       * attacks. This wraps the dojox/html/entities encode function. It is intentionally wrapped so that
       * if we need to make a change (e.g. change the encoding handling) we can make it in one place
       * 
       * @instance
       * @returns The encoded input string
       */
      encodeHTML: function alfresco_core_Core__encodeHTML(textIn) {
         return htmlEntities.encode(textIn);
      },

      /**
       * This is the scope to use within the data model. If this is not initiated during instantiation then
       * it will be assigned to the root scope of the data model the first time any of the data API functions
       * are used. 
       * 
       * @instance
       * @type {object}
       * @default null
       */
      dataScope: null,
      
      /**
       * This will be used to keep track of all the data event callbacks that are registered for the instance.
       * These will be iterated over and removed when the instance is destroyed.
       * 
       * @instance
       * @type {function[]}
       * @default null
       */
      dataBindingCallbacks: null,
      
      alfProcessDataDotNotation: function alfresco_core_Core__alfProcessDataDotNotation(dotNotation) {
         var re = /(\.|\[)/g;
         return dotNotation.replace(re, "._alfValue$1")
      },
      
      /**
       * This both sets data and registers the widget of as the owner of the data. This is done so that 
       * when the widget is destroyed the data it owned will be removed from the data model
       * 
       * @instance
       * @param {string} dotNotation A dot notation representation of the location within the data model to set.
       * @param {object} value The value to set
       * @param {object} scope The scope to set the data at. If null the instance scope will be used.
       * @returns {object} An object that the widget can use to remove the data when it is destroyed.
       */
      alfSetData: function alfresco_core_Core__alfSetData(dotNotation, value, scope) {
         this.alfLog("log", "Setting data", dotNotation, value, scope, this);
         var dataOwnerBinding = {};
         if (this.dataScope == null)
         {
            this.dataScope = CoreData.getSingleton().root;
         }
         if (scope == null)
         {
            scope = this.dataScope;
         }
         
         // Process the dotNotation...
         // Adds in the additional "_alfValue" objects...
         dotNotation = this.alfProcessDataDotNotation(dotNotation);
         
         var data = lang.getObject(dotNotation, false, scope);
         if (data == null)
         {
            // The data item doesn't exist yet, create it now and register the caller
            // as the owner. Not sure if this is necessary, we can't tell if the widget is destroyed
         }
         // Set the new data...
         data = lang.getObject(dotNotation, true, scope);
         var oldValue = data._alfValue;
         lang.setObject(dotNotation + "._alfValue", value, scope);
         
         if (data._alfCallbacks != null)
         {
            // Move all the pending callbacks into the callback property
            for (var callbackId in data._alfCallbacks)
            {
               if (typeof data._alfCallbacks[callbackId] === "function")
               {
                  data._alfCallbacks[callbackId](dotNotation, oldValue, value);
               }
            }
         }
         return dataOwnerBinding;
      },
      
      /**
       * This gets the data from the location in the model defined by the scope. If no explicit scope
       * is provided then the instance scope will be used.
       * 
       * @instance
       * @param {string} dotNotation A dot notation representation of the location within the data model to get
       * @param {object} scope The scope to get the data from. If null then then instance scope will be used.
       * @returns {object} The data at the supplied location
       */
      alfGetData: function alfresco_core_Core__alfGetData(dotNotation, scope) {
         // If a data scope has not been set then get the root data model
         if (this.dataScope == null)
         {
            this.dataScope = CoreData.getSingleton().root;
         }
         if (scope == null)
         {
            scope = this.dataScope;
         }
         dotNotation = this.alfProcessDataDotNotation(dotNotation);
         var data = lang.getObject(dotNotation + "._alfValue", false, scope);
         this.alfLog("log", "Getting data", dotNotation, scope, data, this);
         return data;
      },
      
      /**
       * Binds a callback function to an entry in the data model so that when the data is changed the callback
       * will be executed. This allows widgets to respond to data changes dynamically. A reference to the 
       * call back will be returned and it is important that these callbacks are deleted when the widget
       * is destroyed to prevent memory leaks.
       * 
       * @instance
       * @param {string} dotNotation A dot notation representation of the location with the data model to bind to
       * @param {object} scope The scope to look for the dot notated data at
       * @param {function} callback The function to call when the data is changed
       * @returns {object} A reference to the callback so that it can be removed when the caller is destroyed 
       */
      alfBindDataListener: function alfresco_core_Core__alfBindDataListener(dotNotation, scope, callback) {
         if (dotNotation)
         {
            this.alfLog("log", "Binding data listener", dotNotation, scope, callback, this);
            if (this.dataScope == null)
            {
               this.dataScope = CoreData.getSingleton().root;
            }
            if (scope == null)
            {
               scope = this.dataScope;
            }
            // TODO: Validate the dotNotation??
            dotNotation = this.alfProcessDataDotNotation(dotNotation);
            
            var callbacks = lang.getObject(dotNotation + "._alfCallbacks", true, scope);
            var callbackId = this.generateUuid(); // Create a uuid for the callback
            callbacks[callbackId] = callback;     // Set the callback
            
            // Create and return the binding (this should provide enough information to delete the callback
            // from the data model when the owning widget is destroyed)
            var binding = {
               scope: this.dataScope,
               dotNotation: dotNotation,
               callbackId: callbackId
            };
            if (this.dataBindingCallbacks == null)
            {
               this.dataBindingCallbacks = [];
            }
            this.dataBindingCallbacks.push(binding);
            return binding;
         }
      },
      
      /**
       * @instance
       * @param {object} The binding object
       */
      alfRemoveDataListener: function alfresco_core_Core__alfRemoveDataListener(binding) {
         // Need to check my logic here (!?)
         this.alfLog("log", "Removing data binding", binding);
         try
         {
            var data = lang.getObject(binding.dotNotation, false, binding.scope);
            if (data != null)
            {
               delete data._alfCallbacks[binding.callbackId];
            }
         }
         catch(e)
         {
            this.alfLog("error", "Could not delete data listener binding", binding);
         }
      },
      
      /**
       * A String that is used to prefix all pub/sub communications to ensure that only relevant
       * publications are handled and issued.
       * 
       * @instance
       * @type {string}
       * @default ""
       */
      pubSubScope: "",
      
      /**
       * Used to track of any subscriptions that are made. They will be all be unsubscribed when the 
       * [destroy]{@link module:alfresco/core/Core#destroy} function is called.
       * 
       * @instance
       * @type {array}
       * @default null 
       */
      alfSubscriptions: null,
      
      /**
       * This function wraps the standard Dojo publish function. It should always be used rather than
       * calling the Dojo implementation directly to allow us to make changes to the implementation or
       * to introduce additional features (such as scoping) or updates to the payload.
       * 
       * @instance
       * @param {string} topic The topic on which to publish
       * @param {object} payload The payload to publish on the supplied topic
       * @param {boolean} global Indicates that the pub/sub scope should not be applied
       */
      alfPublish: function alfresco_core_Core__alfPublish(topic, payload, global) {
         var scopedTopic = (global ? "" : this.pubSubScope) + topic;
         if (payload == null)
         {
            payload = {};
         }
         payload.alfTopic = scopedTopic;

         if (AlfConstants.DEBUG == true)
         {
            PubSubLog.getSingleton().pub(scopedTopic, payload, this);
         }

         // Publish...
         pubSub.publish(scopedTopic, payload);
      },
      
      /**
       * This function wraps the standard Dojo subscribe function. It should always be used rather than
       * calling the Dojo implementation directly to allow us to make changes to the implementation or
       * to introduce additional features (such as scoping) or updates to the callback. The subscription
       * handle that gets created is add to [alfSubscriptions]{@link module:alfresco/core/Core#alfSubscriptions}
       * 
       * @instance
       * @param {string} topic The topic on which to subscribe
       * @param {function} callback The callback function to call when the topic is published on.
       * @param {boolean} global Indicates that the pub/sub scope should not be applied
       * @returns {object} A handle to the subscription
       */
      alfSubscribe: function alfresco_core_Core__alfSubscribe(topic, callback, global) {
         var scopedTopic = (global ? "" : this.pubSubScope) + topic;

         if (AlfConstants.DEBUG == true)
         {
            PubSubLog.getSingleton().sub(scopedTopic, callback, this);
         }

         var handle = pubSub.subscribe(scopedTopic, callback);
         if (this.alfSubscriptions == null)
         {
            this.alfSubscriptions = [];
         }
         this.alfSubscriptions.push(handle);
         return handle;
      },
      
      /**
       * This function wraps the standard unsubscribe function. It should always be used rather than call
       * the Dojo implementation directly.
       * 
       * @instance
       * @param {object} The subscription handle to unsubscribe
       */
      alfUnsubscribe: function alfresco_core_Core__alfUnsubscribe(handle) {
         if (handle) 
         {
            if (AlfConstants.DEBUG == true)
            {
               PubSubLog.getSingleton().unsub(handle, this);
            }
            handle.remove();
         }
      },

      /**
       * This function will override a destroy method if available (e.g. if this has been mixed into a
       * widget instance) so that any subscriptions that have been made can be removed. This is necessary
       * because subscriptions are not automatically cleaned up when the widget is destroyed.
       * 
       * This also removes any data binding listeners that have been registered.
       * 
       * @instance
       * @param {boolean} preserveDom
       */
      destroy: function alfresco_core_Core__destroy(preserveDom) {
         if (typeof this.inherited === "function")
         {
            this.inherited(arguments);
         }
         if (this.alfSubscriptions != null)
         {
            array.forEach(this.alfSubscriptions, function(handle, i) {
               if (typeof handle.remove === "function")
               {
                  handle.remove();
               }
            });
         }
         if (this.dataBindingCallbacks != null)
         {
            array.forEach(this.dataBindingCallbacks, function(binding, i) {
               this.alfRemoveDataListener(binding);
            }, this);
         }

         if (this.servicesToDestroy != null)
         {
            array.forEach(this.servicesToDestroy, function(service, i) {
               if (service != null && typeof service.destroy === "function")
               {
                  service.destroy();
               }
            }, this);
         }

         if (this.widgetsToDestroy != null)
         {
            array.forEach(this.widgetsToDestroy, function(widget, i) {
               if (widget != null && typeof widget.destroy === "function")
               {
                  widget.destroy();
               }
            }, this);
         }
      },

      /**
       * This will be used to keep track of all services that are created so that they can be destroyed
       * when the the current instance is destroyed.
       *
       * @instance
       * @type {object[]}
       * @default
       */
      servicesToDestroy: null,

      /**
       * This will be used to keep track of all widgets that are created so that they can be destroyed
       * when the the current instance is destroyed.
       *
       * @instance
       * @type {object[]}
       * @default
       */
      widgetsToDestroy: null,

      /**
       * @instance
       * @type {string}
       * @default "ALF_LOG_REQUEST"
       */
      alfLoggingTopic: "ALF_LOG_REQUEST",
      
      /**
       * This function is intended to provide the entry point to all client-side logging from the application. By 
       * default it simply delegates to the standard browser console object but could optionally be overridden or
       * extended to provide advanced capabilities like posting client-side logs back to the server, etc.
       * 
       * @instance
       * @param {string} severity The severity of the message to be logged
       * @param {string} message The message to be logged
       */
      alfLog: function alfresco_core_Core__alfLog(severity, message) {
         this.alfPublish(this.alfLoggingTopic, {
            callerName: arguments.callee.caller.name,
            severity: severity,
            messageArgs: Array.prototype.slice.call(arguments, 1)
         }, true);
      }
   });
});
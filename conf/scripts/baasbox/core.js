/*
 * Copyright (c) 2014.
 *
 * BaasBox - info@baasbox.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Created by Andrea Tortorella on 23/06/14.
 */

Console.log("Loaded baasbox core");

/**
 * Baasbox server version
 * @type {string}
 */
exports.version = '0.9.0';

var Documents = {};



var log = function(msg){
    var message;
    if(typeof msg === 'string'){
        message = msg;
    } else {
        message = JSON.stringify(msg);
    }

    _command({resource: 'script',
              name: 'log',
              params: ' '+id+' '+message});

};



var runInTransaction = function(fn){
    if(!(typeof fn === 'function'))
        throw new Error("runInTransaction requires a single function argument");
    _command({resource: 'db',
              name: 'transact',
              callback: fn
              });
};

var isInTransaction = function(){
    return _command({resource: 'db',
                     name: 'isInTransaction'});
};

var isAdmin = function(){
    return _command({resource: 'db',
                     name: 'isAdmin'});
};

var runAsAdmin = function(fn) {
    return _command({resource: 'db',
                     name: 'switchUser',
                     callback: fn});
};


Documents.find = function(){
    var coll = null,
        q = null,
        id = null;
    switch (arguments.length){
        case 2:
            if(typeof arguments[1] === 'string') {
                id = arguments[1];
            } else {
                q = arguments[1];
            }
        case 1:
            coll = arguments[0];
    }
    if(!(typeof coll === 'string')){
        throw new TypeError("you must specify a collection");
    }
    if(id === null && (typeof q === 'object')){
        return _command({resource: 'documents',
                         name: 'list',
                         params: {
                             collection: coll,
                             query: q
                         }});
    } else {
        return _command({resource: 'documents',
                         name: 'get',
                         params:{
                             id: id
                         }});
    }
};

Documents.remove = function(coll,id){
    if(!(coll && id)){
        throw new TypeError("missing arguments");
    }
    return _command({resource: 'documents',
                     name: 'delete',
                     params: {

                         collection: 'collection',
                         id: id
                     }});
}

Documents.save = function(){
    var coll = null,
        obj = null,
        id = null;
    if(arguments.length===1 && typeof arguments[0] === 'object'){
        obj = arguments[0];
        coll = obj['@class'];
        id = obj['id'];
    } else if(arguments.length===1 &&
              typeof arguments[0]==='string' &&
              typeof arguments[1]==='object'){
        coll = arguments[0];
        obj = arguments[1];
        id = obj['id'];
    }
    if(!(obj && coll)){
        throw new TypeError("Invalid arguments");
    }
    if(id){
        return _command({resource: 'documents',
                         name: 'put',
                         params: {
                             collection: coll,
                             data: obj,
                             id: id
                         }});
    } else {
        return _command({
            resource: 'documents',
            name: 'post',
            params: {
                collection: '',
                data: {}
            }
        });
    }
};




exports.Documents = Documents;

exports.log = log;

exports.runAsAdmin=runAsAdmin;

exports.runInTransaction=runInTransaction;

exports.isAdmin=isAdmin;

exports.isInTransaction=isInTransaction;

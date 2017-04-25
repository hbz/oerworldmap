/**
 * @license
 * Lodash lodash.com/license | Underscore.js 1.8.3 underscorejs.org/LICENSE
 */
;(function(){function n(n,t){return n.set(t[0],t[1]),n}function t(n,t){return n.add(t),n}function r(n,t,r){switch(r.length){case 0:return n.call(t);case 1:return n.call(t,r[0]);case 2:return n.call(t,r[0],r[1]);case 3:return n.call(t,r[0],r[1],r[2])}return n.apply(t,r)}function e(n,t,r,e){for(var u=-1,i=null==n?0:n.length;++u<i;){var o=n[u];t(e,o,r(o),n)}return e}function u(n,t){for(var r=-1,e=null==n?0:n.length;++r<e&&false!==t(n[r],r,n););return n}function i(n,t){for(var r=null==n?0:n.length;r--&&false!==t(n[r],r,n););
return n}function o(n,t){for(var r=-1,e=null==n?0:n.length;++r<e;)if(!t(n[r],r,n))return false;return true}function f(n,t){for(var r=-1,e=null==n?0:n.length,u=0,i=[];++r<e;){var o=n[r];t(o,r,n)&&(i[u++]=o)}return i}function c(n,t){return!(null==n||!n.length)&&-1<d(n,t,0)}function a(n,t,r){for(var e=-1,u=null==n?0:n.length;++e<u;)if(r(t,n[e]))return true;return false}function l(n,t){for(var r=-1,e=null==n?0:n.length,u=Array(e);++r<e;)u[r]=t(n[r],r,n);return u}function s(n,t){for(var r=-1,e=t.length,u=n.length;++r<e;)n[u+r]=t[r];
return n}function h(n,t,r,e){var u=-1,i=null==n?0:n.length;for(e&&i&&(r=n[++u]);++u<i;)r=t(r,n[u],u,n);return r}function p(n,t,r,e){var u=null==n?0:n.length;for(e&&u&&(r=n[--u]);u--;)r=t(r,n[u],u,n);return r}function _(n,t){for(var r=-1,e=null==n?0:n.length;++r<e;)if(t(n[r],r,n))return true;return false}function v(n,t,r){var e;return r(n,function(n,r,u){if(t(n,r,u))return e=r,false}),e}function g(n,t,r,e){var u=n.length;for(r+=e?1:-1;e?r--:++r<u;)if(t(n[r],r,n))return r;return-1}function d(n,t,r){if(t===t)n:{
--r;for(var e=n.length;++r<e;)if(n[r]===t){n=r;break n}n=-1}else n=g(n,b,r);return n}function y(n,t,r,e){--r;for(var u=n.length;++r<u;)if(e(n[r],t))return r;return-1}function b(n){return n!==n}function x(n,t){var r=null==n?0:n.length;return r?k(n,t)/r:P}function j(n){return function(t){return null==t?F:t[n]}}function w(n){return function(t){return null==n?F:n[t]}}function m(n,t,r,e,u){return u(n,function(n,u,i){r=e?(e=false,n):t(r,n,u,i)}),r}function A(n,t){var r=n.length;for(n.sort(t);r--;)n[r]=n[r].c;
return n}function k(n,t){for(var r,e=-1,u=n.length;++e<u;){var i=t(n[e]);i!==F&&(r=r===F?i:r+i)}return r}function E(n,t){for(var r=-1,e=Array(n);++r<n;)e[r]=t(r);return e}function O(n,t){return l(t,function(t){return[t,n[t]]})}function S(n){return function(t){return n(t)}}function I(n,t){return l(t,function(t){return n[t]})}function R(n,t){return n.has(t)}function z(n,t){for(var r=-1,e=n.length;++r<e&&-1<d(t,n[r],0););return r}function W(n,t){for(var r=n.length;r--&&-1<d(t,n[r],0););return r}function B(n){
return"\\"+Tn[n]}function L(n){var t=-1,r=Array(n.size);return n.forEach(function(n,e){r[++t]=[e,n]}),r}function U(n,t){return function(r){return n(t(r))}}function C(n,t){for(var r=-1,e=n.length,u=0,i=[];++r<e;){var o=n[r];o!==t&&"__lodash_placeholder__"!==o||(n[r]="__lodash_placeholder__",i[u++]=r)}return i}function D(n){var t=-1,r=Array(n.size);return n.forEach(function(n){r[++t]=n}),r}function M(n){var t=-1,r=Array(n.size);return n.forEach(function(n){r[++t]=[n,n]}),r}function T(n){if(Bn.test(n)){
for(var t=zn.lastIndex=0;zn.test(n);)++t;n=t}else n=tt(n);return n}function $(n){return Bn.test(n)?n.match(zn)||[]:n.split("")}var F,N=1/0,P=NaN,Z=[["ary",128],["bind",1],["bindKey",2],["curry",8],["curryRight",16],["flip",512],["partial",32],["partialRight",64],["rearg",256]],q=/\b__p\+='';/g,V=/\b(__p\+=)''\+/g,K=/(__e\(.*?\)|\b__t\))\+'';/g,G=/&(?:amp|lt|gt|quot|#39);/g,H=/[&<>"']/g,J=RegExp(G.source),Y=RegExp(H.source),Q=/<%-([\s\S]+?)%>/g,X=/<%([\s\S]+?)%>/g,nn=/<%=([\s\S]+?)%>/g,tn=/\.|\[(?:[^[\]]*|(["'])(?:(?!\1)[^\\]|\\.)*?\1)\]/,rn=/^\w*$/,en=/^\./,un=/[^.[\]]+|\[(?:(-?\d+(?:\.\d+)?)|(["'])((?:(?!\2)[^\\]|\\.)*?)\2)\]|(?=(?:\.|\[\])(?:\.|\[\]|$))/g,on=/[\\^$.*+?()[\]{}|]/g,fn=RegExp(on.source),cn=/^\s+|\s+$/g,an=/^\s+/,ln=/\s+$/,sn=/\{(?:\n\/\* \[wrapped with .+\] \*\/)?\n?/,hn=/\{\n\/\* \[wrapped with (.+)\] \*/,pn=/,? & /,_n=/[^\x00-\x2f\x3a-\x40\x5b-\x60\x7b-\x7f]+/g,vn=/\\(\\)?/g,gn=/\$\{([^\\}]*(?:\\.[^\\}]*)*)\}/g,dn=/\w*$/,yn=/^[-+]0x[0-9a-f]+$/i,bn=/^0b[01]+$/i,xn=/^\[object .+?Constructor\]$/,jn=/^0o[0-7]+$/i,wn=/^(?:0|[1-9]\d*)$/,mn=/[\xc0-\xd6\xd8-\xf6\xf8-\xff\u0100-\u017f]/g,An=/($^)/,kn=/['\n\r\u2028\u2029\\]/g,En="[\\ufe0e\\ufe0f]?(?:[\\u0300-\\u036f\\ufe20-\\ufe2f\\u20d0-\\u20ff]|\\ud83c[\\udffb-\\udfff])?(?:\\u200d(?:[^\\ud800-\\udfff]|(?:\\ud83c[\\udde6-\\uddff]){2}|[\\ud800-\\udbff][\\udc00-\\udfff])[\\ufe0e\\ufe0f]?(?:[\\u0300-\\u036f\\ufe20-\\ufe2f\\u20d0-\\u20ff]|\\ud83c[\\udffb-\\udfff])?)*",On="(?:[\\u2700-\\u27bf]|(?:\\ud83c[\\udde6-\\uddff]){2}|[\\ud800-\\udbff][\\udc00-\\udfff])"+En,Sn="(?:[^\\ud800-\\udfff][\\u0300-\\u036f\\ufe20-\\ufe2f\\u20d0-\\u20ff]?|[\\u0300-\\u036f\\ufe20-\\ufe2f\\u20d0-\\u20ff]|(?:\\ud83c[\\udde6-\\uddff]){2}|[\\ud800-\\udbff][\\udc00-\\udfff]|[\\ud800-\\udfff])",In=RegExp("['\u2019]","g"),Rn=RegExp("[\\u0300-\\u036f\\ufe20-\\ufe2f\\u20d0-\\u20ff]","g"),zn=RegExp("\\ud83c[\\udffb-\\udfff](?=\\ud83c[\\udffb-\\udfff])|"+Sn+En,"g"),Wn=RegExp(["[A-Z\\xc0-\\xd6\\xd8-\\xde]?[a-z\\xdf-\\xf6\\xf8-\\xff]+(?:['\u2019](?:d|ll|m|re|s|t|ve))?(?=[\\xac\\xb1\\xd7\\xf7\\x00-\\x2f\\x3a-\\x40\\x5b-\\x60\\x7b-\\xbf\\u2000-\\u206f \\t\\x0b\\f\\xa0\\ufeff\\n\\r\\u2028\\u2029\\u1680\\u180e\\u2000\\u2001\\u2002\\u2003\\u2004\\u2005\\u2006\\u2007\\u2008\\u2009\\u200a\\u202f\\u205f\\u3000]|[A-Z\\xc0-\\xd6\\xd8-\\xde]|$)|(?:[A-Z\\xc0-\\xd6\\xd8-\\xde]|[^\\ud800-\\udfff\\xac\\xb1\\xd7\\xf7\\x00-\\x2f\\x3a-\\x40\\x5b-\\x60\\x7b-\\xbf\\u2000-\\u206f \\t\\x0b\\f\\xa0\\ufeff\\n\\r\\u2028\\u2029\\u1680\\u180e\\u2000\\u2001\\u2002\\u2003\\u2004\\u2005\\u2006\\u2007\\u2008\\u2009\\u200a\\u202f\\u205f\\u3000\\d+\\u2700-\\u27bfa-z\\xdf-\\xf6\\xf8-\\xffA-Z\\xc0-\\xd6\\xd8-\\xde])+(?:['\u2019](?:D|LL|M|RE|S|T|VE))?(?=[\\xac\\xb1\\xd7\\xf7\\x00-\\x2f\\x3a-\\x40\\x5b-\\x60\\x7b-\\xbf\\u2000-\\u206f \\t\\x0b\\f\\xa0\\ufeff\\n\\r\\u2028\\u2029\\u1680\\u180e\\u2000\\u2001\\u2002\\u2003\\u2004\\u2005\\u2006\\u2007\\u2008\\u2009\\u200a\\u202f\\u205f\\u3000]|[A-Z\\xc0-\\xd6\\xd8-\\xde](?:[a-z\\xdf-\\xf6\\xf8-\\xff]|[^\\ud800-\\udfff\\xac\\xb1\\xd7\\xf7\\x00-\\x2f\\x3a-\\x40\\x5b-\\x60\\x7b-\\xbf\\u2000-\\u206f \\t\\x0b\\f\\xa0\\ufeff\\n\\r\\u2028\\u2029\\u1680\\u180e\\u2000\\u2001\\u2002\\u2003\\u2004\\u2005\\u2006\\u2007\\u2008\\u2009\\u200a\\u202f\\u205f\\u3000\\d+\\u2700-\\u27bfa-z\\xdf-\\xf6\\xf8-\\xffA-Z\\xc0-\\xd6\\xd8-\\xde])|$)|[A-Z\\xc0-\\xd6\\xd8-\\xde]?(?:[a-z\\xdf-\\xf6\\xf8-\\xff]|[^\\ud800-\\udfff\\xac\\xb1\\xd7\\xf7\\x00-\\x2f\\x3a-\\x40\\x5b-\\x60\\x7b-\\xbf\\u2000-\\u206f \\t\\x0b\\f\\xa0\\ufeff\\n\\r\\u2028\\u2029\\u1680\\u180e\\u2000\\u2001\\u2002\\u2003\\u2004\\u2005\\u2006\\u2007\\u2008\\u2009\\u200a\\u202f\\u205f\\u3000\\d+\\u2700-\\u27bfa-z\\xdf-\\xf6\\xf8-\\xffA-Z\\xc0-\\xd6\\xd8-\\xde])+(?:['\u2019](?:d|ll|m|re|s|t|ve))?|[A-Z\\xc0-\\xd6\\xd8-\\xde]+(?:['\u2019](?:D|LL|M|RE|S|T|VE))?|\\d*(?:(?:1ST|2ND|3RD|(?![123])\\dTH)\\b)|\\d*(?:(?:1st|2nd|3rd|(?![123])\\dth)\\b)|\\d+",On].join("|"),"g"),Bn=RegExp("[\\u200d\\ud800-\\udfff\\u0300-\\u036f\\ufe20-\\ufe2f\\u20d0-\\u20ff\\ufe0e\\ufe0f]"),Ln=/[a-z][A-Z]|[A-Z]{2,}[a-z]|[0-9][a-zA-Z]|[a-zA-Z][0-9]|[^a-zA-Z0-9 ]/,Un="Array Buffer DataView Date Error Float32Array Float64Array Function Int8Array Int16Array Int32Array Map Math Object Promise RegExp Set String Symbol TypeError Uint8Array Uint8ClampedArray Uint16Array Uint32Array WeakMap _ clearTimeout isFinite parseInt setTimeout".split(" "),Cn={};
Cn["[object Float32Array]"]=Cn["[object Float64Array]"]=Cn["[object Int8Array]"]=Cn["[object Int16Array]"]=Cn["[object Int32Array]"]=Cn["[object Uint8Array]"]=Cn["[object Uint8ClampedArray]"]=Cn["[object Uint16Array]"]=Cn["[object Uint32Array]"]=true,Cn["[object Arguments]"]=Cn["[object Array]"]=Cn["[object ArrayBuffer]"]=Cn["[object Boolean]"]=Cn["[object DataView]"]=Cn["[object Date]"]=Cn["[object Error]"]=Cn["[object Function]"]=Cn["[object Map]"]=Cn["[object Number]"]=Cn["[object Object]"]=Cn["[object RegExp]"]=Cn["[object Set]"]=Cn["[object String]"]=Cn["[object WeakMap]"]=false;
var Dn={};Dn["[object Arguments]"]=Dn["[object Array]"]=Dn["[object ArrayBuffer]"]=Dn["[object DataView]"]=Dn["[object Boolean]"]=Dn["[object Date]"]=Dn["[object Float32Array]"]=Dn["[object Float64Array]"]=Dn["[object Int8Array]"]=Dn["[object Int16Array]"]=Dn["[object Int32Array]"]=Dn["[object Map]"]=Dn["[object Number]"]=Dn["[object Object]"]=Dn["[object RegExp]"]=Dn["[object Set]"]=Dn["[object String]"]=Dn["[object Symbol]"]=Dn["[object Uint8Array]"]=Dn["[object Uint8ClampedArray]"]=Dn["[object Uint16Array]"]=Dn["[object Uint32Array]"]=true,
Dn["[object Error]"]=Dn["[object Function]"]=Dn["[object WeakMap]"]=false;var Mn,Tn={"\\":"\\","'":"'","\n":"n","\r":"r","\u2028":"u2028","\u2029":"u2029"},$n=parseFloat,Fn=parseInt,Nn=typeof global=="object"&&global&&global.Object===Object&&global,Pn=typeof self=="object"&&self&&self.Object===Object&&self,Zn=Nn||Pn||Function("return this")(),qn=typeof exports=="object"&&exports&&!exports.nodeType&&exports,Vn=qn&&typeof module=="object"&&module&&!module.nodeType&&module,Kn=Vn&&Vn.exports===qn,Gn=Kn&&Nn.process;
n:{try{Mn=Gn&&Gn.binding&&Gn.binding("util");break n}catch(n){}Mn=void 0}var Hn=Mn&&Mn.isArrayBuffer,Jn=Mn&&Mn.isDate,Yn=Mn&&Mn.isMap,Qn=Mn&&Mn.isRegExp,Xn=Mn&&Mn.isSet,nt=Mn&&Mn.isTypedArray,tt=j("length"),rt=w({"\xc0":"A","\xc1":"A","\xc2":"A","\xc3":"A","\xc4":"A","\xc5":"A","\xe0":"a","\xe1":"a","\xe2":"a","\xe3":"a","\xe4":"a","\xe5":"a","\xc7":"C","\xe7":"c","\xd0":"D","\xf0":"d","\xc8":"E","\xc9":"E","\xca":"E","\xcb":"E","\xe8":"e","\xe9":"e","\xea":"e","\xeb":"e","\xcc":"I","\xcd":"I","\xce":"I",
"\xcf":"I","\xec":"i","\xed":"i","\xee":"i","\xef":"i","\xd1":"N","\xf1":"n","\xd2":"O","\xd3":"O","\xd4":"O","\xd5":"O","\xd6":"O","\xd8":"O","\xf2":"o","\xf3":"o","\xf4":"o","\xf5":"o","\xf6":"o","\xf8":"o","\xd9":"U","\xda":"U","\xdb":"U","\xdc":"U","\xf9":"u","\xfa":"u","\xfb":"u","\xfc":"u","\xdd":"Y","\xfd":"y","\xff":"y","\xc6":"Ae","\xe6":"ae","\xde":"Th","\xfe":"th","\xdf":"ss","\u0100":"A","\u0102":"A","\u0104":"A","\u0101":"a","\u0103":"a","\u0105":"a","\u0106":"C","\u0108":"C","\u010a":"C",
"\u010c":"C","\u0107":"c","\u0109":"c","\u010b":"c","\u010d":"c","\u010e":"D","\u0110":"D","\u010f":"d","\u0111":"d","\u0112":"E","\u0114":"E","\u0116":"E","\u0118":"E","\u011a":"E","\u0113":"e","\u0115":"e","\u0117":"e","\u0119":"e","\u011b":"e","\u011c":"G","\u011e":"G","\u0120":"G","\u0122":"G","\u011d":"g","\u011f":"g","\u0121":"g","\u0123":"g","\u0124":"H","\u0126":"H","\u0125":"h","\u0127":"h","\u0128":"I","\u012a":"I","\u012c":"I","\u012e":"I","\u0130":"I","\u0129":"i","\u012b":"i","\u012d":"i",
"\u012f":"i","\u0131":"i","\u0134":"J","\u0135":"j","\u0136":"K","\u0137":"k","\u0138":"k","\u0139":"L","\u013b":"L","\u013d":"L","\u013f":"L","\u0141":"L","\u013a":"l","\u013c":"l","\u013e":"l","\u0140":"l","\u0142":"l","\u0143":"N","\u0145":"N","\u0147":"N","\u014a":"N","\u0144":"n","\u0146":"n","\u0148":"n","\u014b":"n","\u014c":"O","\u014e":"O","\u0150":"O","\u014d":"o","\u014f":"o","\u0151":"o","\u0154":"R","\u0156":"R","\u0158":"R","\u0155":"r","\u0157":"r","\u0159":"r","\u015a":"S","\u015c":"S",
"\u015e":"S","\u0160":"S","\u015b":"s","\u015d":"s","\u015f":"s","\u0161":"s","\u0162":"T","\u0164":"T","\u0166":"T","\u0163":"t","\u0165":"t","\u0167":"t","\u0168":"U","\u016a":"U","\u016c":"U","\u016e":"U","\u0170":"U","\u0172":"U","\u0169":"u","\u016b":"u","\u016d":"u","\u016f":"u","\u0171":"u","\u0173":"u","\u0174":"W","\u0175":"w","\u0176":"Y","\u0177":"y","\u0178":"Y","\u0179":"Z","\u017b":"Z","\u017d":"Z","\u017a":"z","\u017c":"z","\u017e":"z","\u0132":"IJ","\u0133":"ij","\u0152":"Oe","\u0153":"oe",
"\u0149":"'n","\u017f":"s"}),et=w({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}),ut=w({"&amp;":"&","&lt;":"<","&gt;":">","&quot;":'"',"&#39;":"'"}),it=function w(En){function On(n){if(bu(n)&&!cf(n)&&!(n instanceof Mn)){if(n instanceof zn)return n;if(fi.call(n,"__wrapped__"))return Ne(n)}return new zn(n)}function Sn(){}function zn(n,t){this.__wrapped__=n,this.__actions__=[],this.__chain__=!!t,this.__index__=0,this.__values__=F}function Mn(n){this.__wrapped__=n,this.__actions__=[],this.__dir__=1,
this.__filtered__=false,this.__iteratees__=[],this.__takeCount__=4294967295,this.__views__=[]}function Tn(n){var t=-1,r=null==n?0:n.length;for(this.clear();++t<r;){var e=n[t];this.set(e[0],e[1])}}function Nn(n){var t=-1,r=null==n?0:n.length;for(this.clear();++t<r;){var e=n[t];this.set(e[0],e[1])}}function Pn(n){var t=-1,r=null==n?0:n.length;for(this.clear();++t<r;){var e=n[t];this.set(e[0],e[1])}}function qn(n){var t=-1,r=null==n?0:n.length;for(this.__data__=new Pn;++t<r;)this.add(n[t])}function Vn(n){
this.size=(this.__data__=new Nn(n)).size}function Gn(n,t){var r,e=cf(n),u=!e&&ff(n),i=!e&&!u&&lf(n),o=!e&&!u&&!i&&vf(n),u=(e=e||u||i||o)?E(n.length,ti):[],f=u.length;for(r in n)!t&&!fi.call(n,r)||e&&("length"==r||i&&("offset"==r||"parent"==r)||o&&("buffer"==r||"byteLength"==r||"byteOffset"==r)||Se(r,f))||u.push(r);return u}function tt(n){var t=n.length;return t?n[ar(0,t-1)]:F}function ot(n,t){return Me(Tr(n),dt(t,0,n.length))}function ft(n){return Me(Tr(n))}function ct(n,t,r,e){return n===F||su(n,ui[r])&&!fi.call(e,r)?t:n;
}function at(n,t,r){(r===F||su(n[t],r))&&(r!==F||t in n)||vt(n,t,r)}function lt(n,t,r){var e=n[t];fi.call(n,t)&&su(e,r)&&(r!==F||t in n)||vt(n,t,r)}function st(n,t){for(var r=n.length;r--;)if(su(n[r][0],t))return r;return-1}function ht(n,t,r,e){return io(n,function(n,u,i){t(e,n,r(n),i)}),e}function pt(n,t){return n&&$r(t,Bu(t),n)}function _t(n,t){return n&&$r(t,Lu(t),n)}function vt(n,t,r){"__proto__"==t&&ki?ki(n,t,{configurable:true,enumerable:true,value:r,writable:true}):n[t]=r}function gt(n,t){for(var r=-1,e=t.length,u=Gu(e),i=null==n;++r<e;)u[r]=i?F:zu(n,t[r]);
return u}function dt(n,t,r){return n===n&&(r!==F&&(n=n<=r?n:r),t!==F&&(n=n>=t?n:t)),n}function yt(n,t,r,e,i,o){var f,c=1&t,a=2&t,l=4&t;if(r&&(f=i?r(n,e,i,o):r(n)),f!==F)return f;if(!yu(n))return n;if(e=cf(n)){if(f=Ae(n),!c)return Tr(n,f)}else{var s=go(n),h="[object Function]"==s||"[object GeneratorFunction]"==s;if(lf(n))return Br(n,c);if("[object Object]"==s||"[object Arguments]"==s||h&&!i){if(f=a||h?{}:ke(n),!c)return a?Nr(n,_t(f,n)):Fr(n,pt(f,n))}else{if(!Dn[s])return i?n:{};f=Ee(n,s,yt,c)}}if(o||(o=new Vn),
i=o.get(n))return i;o.set(n,f);var a=l?a?ge:ve:a?Lu:Bu,p=e?F:a(n);return u(p||n,function(e,u){p&&(u=e,e=n[u]),lt(f,u,yt(e,t,r,u,n,o))}),f}function bt(n){var t=Bu(n);return function(r){return xt(r,n,t)}}function xt(n,t,r){var e=r.length;if(null==n)return!e;for(n=Xu(n);e--;){var u=r[e],i=t[u],o=n[u];if(o===F&&!(u in n)||!i(o))return false}return true}function jt(n,t,r){if(typeof n!="function")throw new ri("Expected a function");return xo(function(){n.apply(F,r)},t)}function wt(n,t,r,e){var u=-1,i=c,o=true,f=n.length,s=[],h=t.length;
if(!f)return s;r&&(t=l(t,S(r))),e?(i=a,o=false):200<=t.length&&(i=R,o=false,t=new qn(t));n:for(;++u<f;){var p=n[u],_=null==r?p:r(p),p=e||0!==p?p:0;if(o&&_===_){for(var v=h;v--;)if(t[v]===_)continue n;s.push(p)}else i(t,_,e)||s.push(p)}return s}function mt(n,t){var r=true;return io(n,function(n,e,u){return r=!!t(n,e,u)}),r}function At(n,t,r){for(var e=-1,u=n.length;++e<u;){var i=n[e],o=t(i);if(null!=o&&(f===F?o===o&&!mu(o):r(o,f)))var f=o,c=i}return c}function kt(n,t){var r=[];return io(n,function(n,e,u){
t(n,e,u)&&r.push(n)}),r}function Et(n,t,r,e,u){var i=-1,o=n.length;for(r||(r=Oe),u||(u=[]);++i<o;){var f=n[i];0<t&&r(f)?1<t?Et(f,t-1,r,e,u):s(u,f):e||(u[u.length]=f)}return u}function Ot(n,t){return n&&fo(n,t,Bu)}function St(n,t){return n&&co(n,t,Bu)}function It(n,t){return f(t,function(t){return vu(n[t])})}function Rt(n,t){t=zr(t,n);for(var r=0,e=t.length;null!=n&&r<e;)n=n[Te(t[r++])];return r&&r==e?n:F}function zt(n,t,r){return t=t(n),cf(n)?t:s(t,r(n))}function Wt(n){if(null==n)return n===F?"[object Undefined]":"[object Null]";
n=Xu(n);var t;if(Ai&&Ai in n){var r=fi.call(n,Ai),e=n[Ai];try{n[Ai]=F,t=true}catch(n){}var u=li.call(n);t&&(r?n[Ai]=e:delete n[Ai]),t=u}else t=li.call(n);return t}function Bt(n,t){return n>t}function Lt(n,t){return null!=n&&fi.call(n,t)}function Ut(n,t){return null!=n&&t in Xu(n)}function Ct(n,t,r){for(var e=r?a:c,u=n[0].length,i=n.length,o=i,f=Gu(i),s=1/0,h=[];o--;){var p=n[o];o&&t&&(p=l(p,S(t))),s=Di(p.length,s),f[o]=!r&&(t||120<=u&&120<=p.length)?new qn(o&&p):F}var p=n[0],_=-1,v=f[0];n:for(;++_<u&&h.length<s;){
var g=p[_],d=t?t(g):g,g=r||0!==g?g:0;if(v?!R(v,d):!e(h,d,r)){for(o=i;--o;){var y=f[o];if(y?!R(y,d):!e(n[o],d,r))continue n}v&&v.push(d),h.push(g)}}return h}function Dt(n,t,r){var e={};return Ot(n,function(n,u,i){t(e,r(n),u,i)}),e}function Mt(n,t,e){return t=zr(t,n),n=2>t.length?n:Rt(n,gr(t,0,-1)),t=null==n?n:n[Te(Ke(t))],null==t?F:r(t,n,e)}function Tt(n){return bu(n)&&"[object Arguments]"==Wt(n)}function $t(n){return bu(n)&&"[object ArrayBuffer]"==Wt(n)}function Ft(n){return bu(n)&&"[object Date]"==Wt(n);
}function Nt(n,t,r,e,u){if(n===t)t=true;else if(null==n||null==t||!yu(n)&&!bu(t))t=n!==n&&t!==t;else n:{var i=cf(n),o=cf(t),f="[object Array]",c="[object Array]";i||(f=go(n),f="[object Arguments]"==f?"[object Object]":f),o||(c=go(t),c="[object Arguments]"==c?"[object Object]":c);var a="[object Object]"==f,o="[object Object]"==c;if((c=f==c)&&lf(n)){if(!lf(t)){t=false;break n}i=true,a=false}if(c&&!a)u||(u=new Vn),t=i||vf(n)?he(n,t,r,e,Nt,u):pe(n,t,f,r,e,Nt,u);else{if(!(1&r)&&(i=a&&fi.call(n,"__wrapped__"),f=o&&fi.call(t,"__wrapped__"),
i||f)){n=i?n.value():n,t=f?t.value():t,u||(u=new Vn),t=Nt(n,t,r,e,u);break n}if(c)t:if(u||(u=new Vn),i=1&r,f=Bu(n),o=f.length,c=Bu(t).length,o==c||i){for(a=o;a--;){var l=f[a];if(!(i?l in t:fi.call(t,l))){t=false;break t}}if((c=u.get(n))&&u.get(t))t=c==t;else{c=true,u.set(n,t),u.set(t,n);for(var s=i;++a<o;){var l=f[a],h=n[l],p=t[l];if(e)var _=i?e(p,h,l,t,n,u):e(h,p,l,n,t,u);if(_===F?h!==p&&!Nt(h,p,r,e,u):!_){c=false;break}s||(s="constructor"==l)}c&&!s&&(r=n.constructor,e=t.constructor,r!=e&&"constructor"in n&&"constructor"in t&&!(typeof r=="function"&&r instanceof r&&typeof e=="function"&&e instanceof e)&&(c=false)),
u.delete(n),u.delete(t),t=c}}else t=false;else t=false}}return t}function Pt(n){return bu(n)&&"[object Map]"==go(n)}function Zt(n,t,r,e){var u=r.length,i=u,o=!e;if(null==n)return!i;for(n=Xu(n);u--;){var f=r[u];if(o&&f[2]?f[1]!==n[f[0]]:!(f[0]in n))return false}for(;++u<i;){var f=r[u],c=f[0],a=n[c],l=f[1];if(o&&f[2]){if(a===F&&!(c in n))return false}else{if(f=new Vn,e)var s=e(a,l,c,n,t,f);if(s===F?!Nt(l,a,3,e,f):!s)return false}}return true}function qt(n){return!(!yu(n)||ai&&ai in n)&&(vu(n)?pi:xn).test($e(n))}function Vt(n){
return bu(n)&&"[object RegExp]"==Wt(n)}function Kt(n){return bu(n)&&"[object Set]"==go(n)}function Gt(n){return bu(n)&&du(n.length)&&!!Cn[Wt(n)]}function Ht(n){return typeof n=="function"?n:null==n?Fu:typeof n=="object"?cf(n)?nr(n[0],n[1]):Xt(n):qu(n)}function Jt(n){if(!We(n))return Ui(n);var t,r=[];for(t in Xu(n))fi.call(n,t)&&"constructor"!=t&&r.push(t);return r}function Yt(n,t){return n<t}function Qt(n,t){var r=-1,e=hu(n)?Gu(n.length):[];return io(n,function(n,u,i){e[++r]=t(n,u,i)}),e}function Xt(n){
var t=je(n);return 1==t.length&&t[0][2]?Be(t[0][0],t[0][1]):function(r){return r===n||Zt(r,n,t)}}function nr(n,t){return Re(n)&&t===t&&!yu(t)?Be(Te(n),t):function(r){var e=zu(r,n);return e===F&&e===t?Wu(r,n):Nt(t,e,3)}}function tr(n,t,r,e,u){n!==t&&fo(t,function(i,o){if(yu(i)){u||(u=new Vn);var f=u,c=n[o],a=t[o],l=f.get(a);if(l)at(n,o,l);else{var l=e?e(c,a,o+"",n,t,f):F,s=l===F;if(s){var h=cf(a),p=!h&&lf(a),_=!h&&!p&&vf(a),l=a;h||p||_?cf(c)?l=c:pu(c)?l=Tr(c):p?(s=false,l=Br(a,true)):_?(s=false,l=Ur(a,true)):l=[]:ju(a)||ff(a)?(l=c,
ff(c)?l=Iu(c):(!yu(c)||r&&vu(c))&&(l=ke(a))):s=false}s&&(f.set(a,l),tr(l,a,r,e,f),f.delete(a)),at(n,o,l)}}else f=e?e(n[o],i,o+"",n,t,u):F,f===F&&(f=i),at(n,o,f)},Lu)}function rr(n,t){var r=n.length;if(r)return t+=0>t?r:0,Se(t,r)?n[t]:F}function er(n,t,r){var e=-1;return t=l(t.length?t:[Fu],S(be())),n=Qt(n,function(n){return{a:l(t,function(t){return t(n)}),b:++e,c:n}}),A(n,function(n,t){var e;n:{e=-1;for(var u=n.a,i=t.a,o=u.length,f=r.length;++e<o;){var c=Cr(u[e],i[e]);if(c){e=e>=f?c:c*("desc"==r[e]?-1:1);
break n}}e=n.b-t.b}return e})}function ur(n,t){return n=Xu(n),ir(n,t,function(t,r){return Wu(n,r)})}function ir(n,t,r){for(var e=-1,u=t.length,i={};++e<u;){var o=t[e],f=Rt(n,o);r(f,o)&&_r(i,zr(o,n),f)}return i}function or(n){return function(t){return Rt(t,n)}}function fr(n,t,r,e){var u=e?y:d,i=-1,o=t.length,f=n;for(n===t&&(t=Tr(t)),r&&(f=l(n,S(r)));++i<o;)for(var c=0,a=t[i],a=r?r(a):a;-1<(c=u(f,a,c,e));)f!==n&&ji.call(f,c,1),ji.call(n,c,1);return n}function cr(n,t){for(var r=n?t.length:0,e=r-1;r--;){
var u=t[r];if(r==e||u!==i){var i=u;Se(u)?ji.call(n,u,1):Ar(n,u)}}}function ar(n,t){return n+Ri($i()*(t-n+1))}function lr(n,t){var r="";if(!n||1>t||9007199254740991<t)return r;do t%2&&(r+=n),(t=Ri(t/2))&&(n+=n);while(t);return r}function sr(n,t){return jo(Ue(n,t,Fu),n+"")}function hr(n){return tt(Cu(n))}function pr(n,t){var r=Cu(n);return Me(r,dt(t,0,r.length))}function _r(n,t,r,e){if(!yu(n))return n;t=zr(t,n);for(var u=-1,i=t.length,o=i-1,f=n;null!=f&&++u<i;){var c=Te(t[u]),a=r;if(u!=o){var l=f[c],a=e?e(l,c,f):F;
a===F&&(a=yu(l)?l:Se(t[u+1])?[]:{})}lt(f,c,a),f=f[c]}return n}function vr(n){return Me(Cu(n))}function gr(n,t,r){var e=-1,u=n.length;for(0>t&&(t=-t>u?0:u+t),r=r>u?u:r,0>r&&(r+=u),u=t>r?0:r-t>>>0,t>>>=0,r=Gu(u);++e<u;)r[e]=n[e+t];return r}function dr(n,t){var r;return io(n,function(n,e,u){return r=t(n,e,u),!r}),!!r}function yr(n,t,r){var e=0,u=null==n?e:n.length;if(typeof t=="number"&&t===t&&2147483647>=u){for(;e<u;){var i=e+u>>>1,o=n[i];null!==o&&!mu(o)&&(r?o<=t:o<t)?e=i+1:u=i}return u}return br(n,t,Fu,r);
}function br(n,t,r,e){t=r(t);for(var u=0,i=null==n?0:n.length,o=t!==t,f=null===t,c=mu(t),a=t===F;u<i;){var l=Ri((u+i)/2),s=r(n[l]),h=s!==F,p=null===s,_=s===s,v=mu(s);(o?e||_:a?_&&(e||h):f?_&&h&&(e||!p):c?_&&h&&!p&&(e||!v):p||v?0:e?s<=t:s<t)?u=l+1:i=l}return Di(i,4294967294)}function xr(n,t){for(var r=-1,e=n.length,u=0,i=[];++r<e;){var o=n[r],f=t?t(o):o;if(!r||!su(f,c)){var c=f;i[u++]=0===o?0:o}}return i}function jr(n){return typeof n=="number"?n:mu(n)?P:+n}function wr(n){if(typeof n=="string")return n;
if(cf(n))return l(n,wr)+"";if(mu(n))return eo?eo.call(n):"";var t=n+"";return"0"==t&&1/n==-N?"-0":t}function mr(n,t,r){var e=-1,u=c,i=n.length,o=true,f=[],l=f;if(r)o=false,u=a;else if(200<=i){if(u=t?null:ho(n))return D(u);o=false,u=R,l=new qn}else l=t?[]:f;n:for(;++e<i;){var s=n[e],h=t?t(s):s,s=r||0!==s?s:0;if(o&&h===h){for(var p=l.length;p--;)if(l[p]===h)continue n;t&&l.push(h),f.push(s)}else u(l,h,r)||(l!==f&&l.push(h),f.push(s))}return f}function Ar(n,t){return t=zr(t,n),n=2>t.length?n:Rt(n,gr(t,0,-1)),
null==n||delete n[Te(Ke(t))]}function kr(n,t,r,e){for(var u=n.length,i=e?u:-1;(e?i--:++i<u)&&t(n[i],i,n););return r?gr(n,e?0:i,e?i+1:u):gr(n,e?i+1:0,e?u:i)}function Er(n,t){var r=n;return r instanceof Mn&&(r=r.value()),h(t,function(n,t){return t.func.apply(t.thisArg,s([n],t.args))},r)}function Or(n,t,r){var e=n.length;if(2>e)return e?mr(n[0]):[];for(var u=-1,i=Gu(e);++u<e;)for(var o=n[u],f=-1;++f<e;)f!=u&&(i[u]=wt(i[u]||o,n[f],t,r));return mr(Et(i,1),t,r)}function Sr(n,t,r){for(var e=-1,u=n.length,i=t.length,o={};++e<u;)r(o,n[e],e<i?t[e]:F);
return o}function Ir(n){return pu(n)?n:[]}function Rr(n){return typeof n=="function"?n:Fu}function zr(n,t){return cf(n)?n:Re(n,t)?[n]:wo(Ru(n))}function Wr(n,t,r){var e=n.length;return r=r===F?e:r,!t&&r>=e?n:gr(n,t,r)}function Br(n,t){if(t)return n.slice();var r=n.length,r=di?di(r):new n.constructor(r);return n.copy(r),r}function Lr(n){var t=new n.constructor(n.byteLength);return new gi(t).set(new gi(n)),t}function Ur(n,t){return new n.constructor(t?Lr(n.buffer):n.buffer,n.byteOffset,n.length)}function Cr(n,t){
if(n!==t){var r=n!==F,e=null===n,u=n===n,i=mu(n),o=t!==F,f=null===t,c=t===t,a=mu(t);if(!f&&!a&&!i&&n>t||i&&o&&c&&!f&&!a||e&&o&&c||!r&&c||!u)return 1;if(!e&&!i&&!a&&n<t||a&&r&&u&&!e&&!i||f&&r&&u||!o&&u||!c)return-1}return 0}function Dr(n,t,r,e){var u=-1,i=n.length,o=r.length,f=-1,c=t.length,a=Ci(i-o,0),l=Gu(c+a);for(e=!e;++f<c;)l[f]=t[f];for(;++u<o;)(e||u<i)&&(l[r[u]]=n[u]);for(;a--;)l[f++]=n[u++];return l}function Mr(n,t,r,e){var u=-1,i=n.length,o=-1,f=r.length,c=-1,a=t.length,l=Ci(i-f,0),s=Gu(l+a);
for(e=!e;++u<l;)s[u]=n[u];for(l=u;++c<a;)s[l+c]=t[c];for(;++o<f;)(e||u<i)&&(s[l+r[o]]=n[u++]);return s}function Tr(n,t){var r=-1,e=n.length;for(t||(t=Gu(e));++r<e;)t[r]=n[r];return t}function $r(n,t,r,e){var u=!r;r||(r={});for(var i=-1,o=t.length;++i<o;){var f=t[i],c=e?e(r[f],n[f],f,r,n):F;c===F&&(c=n[f]),u?vt(r,f,c):lt(r,f,c)}return r}function Fr(n,t){return $r(n,_o(n),t)}function Nr(n,t){return $r(n,vo(n),t)}function Pr(n,t){return function(r,u){var i=cf(r)?e:ht,o=t?t():{};return i(r,n,be(u,2),o);
}}function Zr(n){return sr(function(t,r){var e=-1,u=r.length,i=1<u?r[u-1]:F,o=2<u?r[2]:F,i=3<n.length&&typeof i=="function"?(u--,i):F;for(o&&Ie(r[0],r[1],o)&&(i=3>u?F:i,u=1),t=Xu(t);++e<u;)(o=r[e])&&n(t,o,e,i);return t})}function qr(n,t){return function(r,e){if(null==r)return r;if(!hu(r))return n(r,e);for(var u=r.length,i=t?u:-1,o=Xu(r);(t?i--:++i<u)&&false!==e(o[i],i,o););return r}}function Vr(n){return function(t,r,e){var u=-1,i=Xu(t);e=e(t);for(var o=e.length;o--;){var f=e[n?o:++u];if(false===r(i[f],f,i))break;
}return t}}function Kr(n,t,r){function e(){return(this&&this!==Zn&&this instanceof e?i:n).apply(u?r:this,arguments)}var u=1&t,i=Jr(n);return e}function Gr(n){return function(t){t=Ru(t);var r=Bn.test(t)?$(t):F,e=r?r[0]:t.charAt(0);return t=r?Wr(r,1).join(""):t.slice(1),e[n]()+t}}function Hr(n){return function(t){return h(Tu(Mu(t).replace(In,"")),n,"")}}function Jr(n){return function(){var t=arguments;switch(t.length){case 0:return new n;case 1:return new n(t[0]);case 2:return new n(t[0],t[1]);case 3:
return new n(t[0],t[1],t[2]);case 4:return new n(t[0],t[1],t[2],t[3]);case 5:return new n(t[0],t[1],t[2],t[3],t[4]);case 6:return new n(t[0],t[1],t[2],t[3],t[4],t[5]);case 7:return new n(t[0],t[1],t[2],t[3],t[4],t[5],t[6])}var r=uo(n.prototype),t=n.apply(r,t);return yu(t)?t:r}}function Yr(n,t,e){function u(){for(var o=arguments.length,f=Gu(o),c=o,a=ye(u);c--;)f[c]=arguments[c];return c=3>o&&f[0]!==a&&f[o-1]!==a?[]:C(f,a),o-=c.length,o<e?ce(n,t,ne,u.placeholder,F,f,c,F,F,e-o):r(this&&this!==Zn&&this instanceof u?i:n,this,f);
}var i=Jr(n);return u}function Qr(n){return function(t,r,e){var u=Xu(t);if(!hu(t)){var i=be(r,3);t=Bu(t),r=function(n){return i(u[n],n,u)}}return r=n(t,r,e),-1<r?u[i?t[r]:r]:F}}function Xr(n){return _e(function(t){var r=t.length,e=r,u=zn.prototype.thru;for(n&&t.reverse();e--;){var i=t[e];if(typeof i!="function")throw new ri("Expected a function");if(u&&!o&&"wrapper"==de(i))var o=new zn([],true)}for(e=o?e:r;++e<r;)var i=t[e],u=de(i),f="wrapper"==u?po(i):F,o=f&&ze(f[0])&&424==f[1]&&!f[4].length&&1==f[9]?o[de(f[0])].apply(o,f[3]):1==i.length&&ze(i)?o[u]():o.thru(i);
return function(){var n=arguments,e=n[0];if(o&&1==n.length&&cf(e)&&200<=e.length)return o.plant(e).value();for(var u=0,n=r?t[u].apply(this,n):e;++u<r;)n=t[u].call(this,n);return n}})}function ne(n,t,r,e,u,i,o,f,c,a){function l(){for(var d=arguments.length,y=Gu(d),b=d;b--;)y[b]=arguments[b];if(_){var x,j=ye(l),b=y.length;for(x=0;b--;)y[b]===j&&++x}if(e&&(y=Dr(y,e,u,_)),i&&(y=Mr(y,i,o,_)),d-=x,_&&d<a)return j=C(y,j),ce(n,t,ne,l.placeholder,r,y,j,f,c,a-d);if(j=h?r:this,b=p?j[n]:n,d=y.length,f){x=y.length;
for(var w=Di(f.length,x),m=Tr(y);w--;){var A=f[w];y[w]=Se(A,x)?m[A]:F}}else v&&1<d&&y.reverse();return s&&c<d&&(y.length=c),this&&this!==Zn&&this instanceof l&&(b=g||Jr(b)),b.apply(j,y)}var s=128&t,h=1&t,p=2&t,_=24&t,v=512&t,g=p?F:Jr(n);return l}function te(n,t){return function(r,e){return Dt(r,n,t(e))}}function re(n,t){return function(r,e){var u;if(r===F&&e===F)return t;if(r!==F&&(u=r),e!==F){if(u===F)return e;typeof r=="string"||typeof e=="string"?(r=wr(r),e=wr(e)):(r=jr(r),e=jr(e)),u=n(r,e)}return u;
}}function ee(n){return _e(function(t){return t=l(t,S(be())),sr(function(e){var u=this;return n(t,function(n){return r(n,u,e)})})})}function ue(n,t){t=t===F?" ":wr(t);var r=t.length;return 2>r?r?lr(t,n):t:(r=lr(t,Ii(n/T(t))),Bn.test(t)?Wr($(r),0,n).join(""):r.slice(0,n))}function ie(n,t,e,u){function i(){for(var t=-1,c=arguments.length,a=-1,l=u.length,s=Gu(l+c),h=this&&this!==Zn&&this instanceof i?f:n;++a<l;)s[a]=u[a];for(;c--;)s[a++]=arguments[++t];return r(h,o?e:this,s)}var o=1&t,f=Jr(n);return i;
}function oe(n){return function(t,r,e){e&&typeof e!="number"&&Ie(t,r,e)&&(r=e=F),t=ku(t),r===F?(r=t,t=0):r=ku(r),e=e===F?t<r?1:-1:ku(e);var u=-1;r=Ci(Ii((r-t)/(e||1)),0);for(var i=Gu(r);r--;)i[n?r:++u]=t,t+=e;return i}}function fe(n){return function(t,r){return typeof t=="string"&&typeof r=="string"||(t=Su(t),r=Su(r)),n(t,r)}}function ce(n,t,r,e,u,i,o,f,c,a){var l=8&t,s=l?o:F;o=l?F:o;var h=l?i:F;return i=l?F:i,t=(t|(l?32:64))&~(l?64:32),4&t||(t&=-4),u=[n,t,u,h,s,i,o,f,c,a],r=r.apply(F,u),ze(n)&&bo(r,u),
r.placeholder=e,Ce(r,n,t)}function ae(n){var t=Qu[n];return function(n,r){if(n=Su(n),r=Di(Eu(r),292)){var e=(Ru(n)+"e").split("e"),e=t(e[0]+"e"+(+e[1]+r)),e=(Ru(e)+"e").split("e");return+(e[0]+"e"+(+e[1]-r))}return t(n)}}function le(n){return function(t){var r=go(t);return"[object Map]"==r?L(t):"[object Set]"==r?M(t):O(t,n(t))}}function se(n,t,r,e,u,i,o,f){var c=2&t;if(!c&&typeof n!="function")throw new ri("Expected a function");var a=e?e.length:0;if(a||(t&=-97,e=u=F),o=o===F?o:Ci(Eu(o),0),f=f===F?f:Eu(f),
a-=u?u.length:0,64&t){var l=e,s=u;e=u=F}var h=c?F:po(n);return i=[n,t,r,e,u,l,s,i,o,f],h&&(r=i[1],n=h[1],t=r|n,e=128==n&&8==r||128==n&&256==r&&i[7].length<=h[8]||384==n&&h[7].length<=h[8]&&8==r,131>t||e)&&(1&n&&(i[2]=h[2],t|=1&r?0:4),(r=h[3])&&(e=i[3],i[3]=e?Dr(e,r,h[4]):r,i[4]=e?C(i[3],"__lodash_placeholder__"):h[4]),(r=h[5])&&(e=i[5],i[5]=e?Mr(e,r,h[6]):r,i[6]=e?C(i[5],"__lodash_placeholder__"):h[6]),(r=h[7])&&(i[7]=r),128&n&&(i[8]=null==i[8]?h[8]:Di(i[8],h[8])),null==i[9]&&(i[9]=h[9]),i[0]=h[0],
i[1]=t),n=i[0],t=i[1],r=i[2],e=i[3],u=i[4],f=i[9]=null==i[9]?c?0:n.length:Ci(i[9]-a,0),!f&&24&t&&(t&=-25),Ce((h?ao:bo)(t&&1!=t?8==t||16==t?Yr(n,t,f):32!=t&&33!=t||u.length?ne.apply(F,i):ie(n,t,r,e):Kr(n,t,r),i),n,t)}function he(n,t,r,e,u,i){var o=1&r,f=n.length,c=t.length;if(f!=c&&!(o&&c>f))return false;if((c=i.get(n))&&i.get(t))return c==t;var c=-1,a=true,l=2&r?new qn:F;for(i.set(n,t),i.set(t,n);++c<f;){var s=n[c],h=t[c];if(e)var p=o?e(h,s,c,t,n,i):e(s,h,c,n,t,i);if(p!==F){if(p)continue;a=false;break}if(l){
if(!_(t,function(n,t){if(!R(l,t)&&(s===n||u(s,n,r,e,i)))return l.push(t)})){a=false;break}}else if(s!==h&&!u(s,h,r,e,i)){a=false;break}}return i.delete(n),i.delete(t),a}function pe(n,t,r,e,u,i,o){switch(r){case"[object DataView]":if(n.byteLength!=t.byteLength||n.byteOffset!=t.byteOffset)break;n=n.buffer,t=t.buffer;case"[object ArrayBuffer]":if(n.byteLength!=t.byteLength||!i(new gi(n),new gi(t)))break;return true;case"[object Boolean]":case"[object Date]":case"[object Number]":return su(+n,+t);case"[object Error]":
return n.name==t.name&&n.message==t.message;case"[object RegExp]":case"[object String]":return n==t+"";case"[object Map]":var f=L;case"[object Set]":if(f||(f=D),n.size!=t.size&&!(1&e))break;return(r=o.get(n))?r==t:(e|=2,o.set(n,t),t=he(f(n),f(t),e,u,i,o),o.delete(n),t);case"[object Symbol]":if(ro)return ro.call(n)==ro.call(t)}return false}function _e(n){return jo(Ue(n,F,qe),n+"")}function ve(n){return zt(n,Bu,_o)}function ge(n){return zt(n,Lu,vo)}function de(n){for(var t=n.name+"",r=Hi[t],e=fi.call(Hi,t)?r.length:0;e--;){
var u=r[e],i=u.func;if(null==i||i==n)return u.name}return t}function ye(n){return(fi.call(On,"placeholder")?On:n).placeholder}function be(){var n=On.iteratee||Nu,n=n===Nu?Ht:n;return arguments.length?n(arguments[0],arguments[1]):n}function xe(n,t){var r=n.__data__,e=typeof t;return("string"==e||"number"==e||"symbol"==e||"boolean"==e?"__proto__"!==t:null===t)?r[typeof t=="string"?"string":"hash"]:r.map}function je(n){for(var t=Bu(n),r=t.length;r--;){var e=t[r],u=n[e];t[r]=[e,u,u===u&&!yu(u)]}return t;
}function we(n,t){var r=null==n?F:n[t];return qt(r)?r:F}function me(n,t,r){t=zr(t,n);for(var e=-1,u=t.length,i=false;++e<u;){var o=Te(t[e]);if(!(i=null!=n&&r(n,o)))break;n=n[o]}return i||++e!=u?i:(u=null==n?0:n.length,!!u&&du(u)&&Se(o,u)&&(cf(n)||ff(n)))}function Ae(n){var t=n.length,r=n.constructor(t);return t&&"string"==typeof n[0]&&fi.call(n,"index")&&(r.index=n.index,r.input=n.input),r}function ke(n){return typeof n.constructor!="function"||We(n)?{}:uo(yi(n))}function Ee(r,e,u,i){var o=r.constructor;
switch(e){case"[object ArrayBuffer]":return Lr(r);case"[object Boolean]":case"[object Date]":return new o(+r);case"[object DataView]":return e=i?Lr(r.buffer):r.buffer,new r.constructor(e,r.byteOffset,r.byteLength);case"[object Float32Array]":case"[object Float64Array]":case"[object Int8Array]":case"[object Int16Array]":case"[object Int32Array]":case"[object Uint8Array]":case"[object Uint8ClampedArray]":case"[object Uint16Array]":case"[object Uint32Array]":return Ur(r,i);case"[object Map]":return e=i?u(L(r),1):L(r),
h(e,n,new r.constructor);case"[object Number]":case"[object String]":return new o(r);case"[object RegExp]":return e=new r.constructor(r.source,dn.exec(r)),e.lastIndex=r.lastIndex,e;case"[object Set]":return e=i?u(D(r),1):D(r),h(e,t,new r.constructor);case"[object Symbol]":return ro?Xu(ro.call(r)):{}}}function Oe(n){return cf(n)||ff(n)||!!(wi&&n&&n[wi])}function Se(n,t){return t=null==t?9007199254740991:t,!!t&&(typeof n=="number"||wn.test(n))&&-1<n&&0==n%1&&n<t}function Ie(n,t,r){if(!yu(r))return false;
var e=typeof t;return!!("number"==e?hu(r)&&Se(t,r.length):"string"==e&&t in r)&&su(r[t],n)}function Re(n,t){if(cf(n))return false;var r=typeof n;return!("number"!=r&&"symbol"!=r&&"boolean"!=r&&null!=n&&!mu(n))||(rn.test(n)||!tn.test(n)||null!=t&&n in Xu(t))}function ze(n){var t=de(n),r=On[t];return typeof r=="function"&&t in Mn.prototype&&(n===r||(t=po(r),!!t&&n===t[0]))}function We(n){var t=n&&n.constructor;return n===(typeof t=="function"&&t.prototype||ui)}function Be(n,t){return function(r){return null!=r&&(r[n]===t&&(t!==F||n in Xu(r)));
}}function Le(n,t,r,e,u,i){return yu(n)&&yu(t)&&(i.set(t,n),tr(n,t,F,Le,i),i.delete(t)),n}function Ue(n,t,e){return t=Ci(t===F?n.length-1:t,0),function(){for(var u=arguments,i=-1,o=Ci(u.length-t,0),f=Gu(o);++i<o;)f[i]=u[t+i];for(i=-1,o=Gu(t+1);++i<t;)o[i]=u[i];return o[t]=e(f),r(n,this,o)}}function Ce(n,t,r){var e=t+"";t=jo;var u,i=Fe;return u=(u=e.match(hn))?u[1].split(pn):[],r=i(u,r),(i=r.length)&&(u=i-1,r[u]=(1<i?"& ":"")+r[u],r=r.join(2<i?", ":" "),e=e.replace(sn,"{\n/* [wrapped with "+r+"] */\n")),
t(n,e)}function De(n){var t=0,r=0;return function(){var e=Mi(),u=16-(e-r);if(r=e,0<u){if(800<=++t)return arguments[0]}else t=0;return n.apply(F,arguments)}}function Me(n,t){var r=-1,e=n.length,u=e-1;for(t=t===F?e:t;++r<t;){var e=ar(r,u),i=n[e];n[e]=n[r],n[r]=i}return n.length=t,n}function Te(n){if(typeof n=="string"||mu(n))return n;var t=n+"";return"0"==t&&1/n==-N?"-0":t}function $e(n){if(null!=n){try{return oi.call(n)}catch(n){}return n+""}return""}function Fe(n,t){return u(Z,function(r){var e="_."+r[0];
t&r[1]&&!c(n,e)&&n.push(e)}),n.sort()}function Ne(n){if(n instanceof Mn)return n.clone();var t=new zn(n.__wrapped__,n.__chain__);return t.__actions__=Tr(n.__actions__),t.__index__=n.__index__,t.__values__=n.__values__,t}function Pe(n,t,r){var e=null==n?0:n.length;return e?(r=null==r?0:Eu(r),0>r&&(r=Ci(e+r,0)),g(n,be(t,3),r)):-1}function Ze(n,t,r){var e=null==n?0:n.length;if(!e)return-1;var u=e-1;return r!==F&&(u=Eu(r),u=0>r?Ci(e+u,0):Di(u,e-1)),g(n,be(t,3),u,true)}function qe(n){return(null==n?0:n.length)?Et(n,1):[];
}function Ve(n){return n&&n.length?n[0]:F}function Ke(n){var t=null==n?0:n.length;return t?n[t-1]:F}function Ge(n,t){return n&&n.length&&t&&t.length?fr(n,t):n}function He(n){return null==n?n:Fi.call(n)}function Je(n){if(!n||!n.length)return[];var t=0;return n=f(n,function(n){if(pu(n))return t=Ci(n.length,t),true}),E(t,function(t){return l(n,j(t))})}function Ye(n,t){if(!n||!n.length)return[];var e=Je(n);return null==t?e:l(e,function(n){return r(t,F,n)})}function Qe(n){return n=On(n),n.__chain__=true,n;
}function Xe(n,t){return t(n)}function nu(){return this}function tu(n,t){return(cf(n)?u:io)(n,be(t,3))}function ru(n,t){return(cf(n)?i:oo)(n,be(t,3))}function eu(n,t){return(cf(n)?l:Qt)(n,be(t,3))}function uu(n,t,r){return t=r?F:t,t=n&&null==t?n.length:t,se(n,128,F,F,F,F,t)}function iu(n,t){var r;if(typeof t!="function")throw new ri("Expected a function");return n=Eu(n),function(){return 0<--n&&(r=t.apply(this,arguments)),1>=n&&(t=F),r}}function ou(n,t,r){return t=r?F:t,n=se(n,8,F,F,F,F,F,t),n.placeholder=ou.placeholder,
n}function fu(n,t,r){return t=r?F:t,n=se(n,16,F,F,F,F,F,t),n.placeholder=fu.placeholder,n}function cu(n,t,r){function e(t){var r=c,e=a;return c=a=F,_=t,s=n.apply(e,r)}function u(n){var r=n-p;return n-=_,p===F||r>=t||0>r||g&&n>=l}function i(){var n=Ho();if(u(n))return o(n);var r,e=xo;r=n-_,n=t-(n-p),r=g?Di(n,l-r):n,h=e(i,r)}function o(n){return h=F,d&&c?e(n):(c=a=F,s)}function f(){var n=Ho(),r=u(n);if(c=arguments,a=this,p=n,r){if(h===F)return _=n=p,h=xo(i,t),v?e(n):s;if(g)return h=xo(i,t),e(p)}return h===F&&(h=xo(i,t)),
s}var c,a,l,s,h,p,_=0,v=false,g=false,d=true;if(typeof n!="function")throw new ri("Expected a function");return t=Su(t)||0,yu(r)&&(v=!!r.leading,l=(g="maxWait"in r)?Ci(Su(r.maxWait)||0,t):l,d="trailing"in r?!!r.trailing:d),f.cancel=function(){h!==F&&so(h),_=0,c=p=a=h=F},f.flush=function(){return h===F?s:o(Ho())},f}function au(n,t){function r(){var e=arguments,u=t?t.apply(this,e):e[0],i=r.cache;return i.has(u)?i.get(u):(e=n.apply(this,e),r.cache=i.set(u,e)||i,e)}if(typeof n!="function"||null!=t&&typeof t!="function")throw new ri("Expected a function");
return r.cache=new(au.Cache||Pn),r}function lu(n){if(typeof n!="function")throw new ri("Expected a function");return function(){var t=arguments;switch(t.length){case 0:return!n.call(this);case 1:return!n.call(this,t[0]);case 2:return!n.call(this,t[0],t[1]);case 3:return!n.call(this,t[0],t[1],t[2])}return!n.apply(this,t)}}function su(n,t){return n===t||n!==n&&t!==t}function hu(n){return null!=n&&du(n.length)&&!vu(n)}function pu(n){return bu(n)&&hu(n)}function _u(n){if(!bu(n))return false;var t=Wt(n);return"[object Error]"==t||"[object DOMException]"==t||typeof n.message=="string"&&typeof n.name=="string"&&!ju(n);
}function vu(n){return!!yu(n)&&(n=Wt(n),"[object Function]"==n||"[object GeneratorFunction]"==n||"[object AsyncFunction]"==n||"[object Proxy]"==n)}function gu(n){return typeof n=="number"&&n==Eu(n)}function du(n){return typeof n=="number"&&-1<n&&0==n%1&&9007199254740991>=n}function yu(n){var t=typeof n;return null!=n&&("object"==t||"function"==t)}function bu(n){return null!=n&&typeof n=="object"}function xu(n){return typeof n=="number"||bu(n)&&"[object Number]"==Wt(n)}function ju(n){return!(!bu(n)||"[object Object]"!=Wt(n))&&(n=yi(n),
null===n||(n=fi.call(n,"constructor")&&n.constructor,typeof n=="function"&&n instanceof n&&oi.call(n)==si))}function wu(n){return typeof n=="string"||!cf(n)&&bu(n)&&"[object String]"==Wt(n)}function mu(n){return typeof n=="symbol"||bu(n)&&"[object Symbol]"==Wt(n)}function Au(n){if(!n)return[];if(hu(n))return wu(n)?$(n):Tr(n);if(mi&&n[mi]){n=n[mi]();for(var t,r=[];!(t=n.next()).done;)r.push(t.value);return r}return t=go(n),("[object Map]"==t?L:"[object Set]"==t?D:Cu)(n)}function ku(n){return n?(n=Su(n),
n===N||n===-N?1.7976931348623157e308*(0>n?-1:1):n===n?n:0):0===n?n:0}function Eu(n){n=ku(n);var t=n%1;return n===n?t?n-t:n:0}function Ou(n){return n?dt(Eu(n),0,4294967295):0}function Su(n){if(typeof n=="number")return n;if(mu(n))return P;if(yu(n)&&(n=typeof n.valueOf=="function"?n.valueOf():n,n=yu(n)?n+"":n),typeof n!="string")return 0===n?n:+n;n=n.replace(cn,"");var t=bn.test(n);return t||jn.test(n)?Fn(n.slice(2),t?2:8):yn.test(n)?P:+n}function Iu(n){return $r(n,Lu(n))}function Ru(n){return null==n?"":wr(n);
}function zu(n,t,r){return n=null==n?F:Rt(n,t),n===F?r:n}function Wu(n,t){return null!=n&&me(n,t,Ut)}function Bu(n){return hu(n)?Gn(n):Jt(n)}function Lu(n){if(hu(n))n=Gn(n,true);else if(yu(n)){var t,r=We(n),e=[];for(t in n)("constructor"!=t||!r&&fi.call(n,t))&&e.push(t);n=e}else{if(t=[],null!=n)for(r in Xu(n))t.push(r);n=t}return n}function Uu(n,t){if(null==n)return{};var r=l(ge(n),function(n){return[n]});return t=be(t),ir(n,r,function(n,r){return t(n,r[0])})}function Cu(n){return null==n?[]:I(n,Bu(n));
}function Du(n){return Ff(Ru(n).toLowerCase())}function Mu(n){return(n=Ru(n))&&n.replace(mn,rt).replace(Rn,"")}function Tu(n,t,r){return n=Ru(n),t=r?F:t,t===F?Ln.test(n)?n.match(Wn)||[]:n.match(_n)||[]:n.match(t)||[]}function $u(n){return function(){return n}}function Fu(n){return n}function Nu(n){return Ht(typeof n=="function"?n:yt(n,1))}function Pu(n,t,r){var e=Bu(t),i=It(t,e);null!=r||yu(t)&&(i.length||!e.length)||(r=t,t=n,n=this,i=It(t,Bu(t)));var o=!(yu(r)&&"chain"in r&&!r.chain),f=vu(n);return u(i,function(r){
var e=t[r];n[r]=e,f&&(n.prototype[r]=function(){var t=this.__chain__;if(o||t){var r=n(this.__wrapped__);return(r.__actions__=Tr(this.__actions__)).push({func:e,args:arguments,thisArg:n}),r.__chain__=t,r}return e.apply(n,s([this.value()],arguments))})}),n}function Zu(){}function qu(n){return Re(n)?j(Te(n)):or(n)}function Vu(){return[]}function Ku(){return false}En=null==En?Zn:it.defaults(Zn.Object(),En,it.pick(Zn,Un));var Gu=En.Array,Hu=En.Date,Ju=En.Error,Yu=En.Function,Qu=En.Math,Xu=En.Object,ni=En.RegExp,ti=En.String,ri=En.TypeError,ei=Gu.prototype,ui=Xu.prototype,ii=En["__core-js_shared__"],oi=Yu.prototype.toString,fi=ui.hasOwnProperty,ci=0,ai=function(){
var n=/[^.]+$/.exec(ii&&ii.keys&&ii.keys.IE_PROTO||"");return n?"Symbol(src)_1."+n:""}(),li=ui.toString,si=oi.call(Xu),hi=Zn._,pi=ni("^"+oi.call(fi).replace(on,"\\$&").replace(/hasOwnProperty|(function).*?(?=\\\()| for .+?(?=\\\])/g,"$1.*?")+"$"),_i=Kn?En.Buffer:F,vi=En.Symbol,gi=En.Uint8Array,di=_i?_i.f:F,yi=U(Xu.getPrototypeOf,Xu),bi=Xu.create,xi=ui.propertyIsEnumerable,ji=ei.splice,wi=vi?vi.isConcatSpreadable:F,mi=vi?vi.iterator:F,Ai=vi?vi.toStringTag:F,ki=function(){try{var n=we(Xu,"defineProperty");
return n({},"",{}),n}catch(n){}}(),Ei=En.clearTimeout!==Zn.clearTimeout&&En.clearTimeout,Oi=Hu&&Hu.now!==Zn.Date.now&&Hu.now,Si=En.setTimeout!==Zn.setTimeout&&En.setTimeout,Ii=Qu.ceil,Ri=Qu.floor,zi=Xu.getOwnPropertySymbols,Wi=_i?_i.isBuffer:F,Bi=En.isFinite,Li=ei.join,Ui=U(Xu.keys,Xu),Ci=Qu.max,Di=Qu.min,Mi=Hu.now,Ti=En.parseInt,$i=Qu.random,Fi=ei.reverse,Ni=we(En,"DataView"),Pi=we(En,"Map"),Zi=we(En,"Promise"),qi=we(En,"Set"),Vi=we(En,"WeakMap"),Ki=we(Xu,"create"),Gi=Vi&&new Vi,Hi={},Ji=$e(Ni),Yi=$e(Pi),Qi=$e(Zi),Xi=$e(qi),no=$e(Vi),to=vi?vi.prototype:F,ro=to?to.valueOf:F,eo=to?to.toString:F,uo=function(){
function n(){}return function(t){return yu(t)?bi?bi(t):(n.prototype=t,t=new n,n.prototype=F,t):{}}}();On.templateSettings={escape:Q,evaluate:X,interpolate:nn,variable:"",imports:{_:On}},On.prototype=Sn.prototype,On.prototype.constructor=On,zn.prototype=uo(Sn.prototype),zn.prototype.constructor=zn,Mn.prototype=uo(Sn.prototype),Mn.prototype.constructor=Mn,Tn.prototype.clear=function(){this.__data__=Ki?Ki(null):{},this.size=0},Tn.prototype.delete=function(n){return n=this.has(n)&&delete this.__data__[n],
this.size-=n?1:0,n},Tn.prototype.get=function(n){var t=this.__data__;return Ki?(n=t[n],"__lodash_hash_undefined__"===n?F:n):fi.call(t,n)?t[n]:F},Tn.prototype.has=function(n){var t=this.__data__;return Ki?t[n]!==F:fi.call(t,n)},Tn.prototype.set=function(n,t){var r=this.__data__;return this.size+=this.has(n)?0:1,r[n]=Ki&&t===F?"__lodash_hash_undefined__":t,this},Nn.prototype.clear=function(){this.__data__=[],this.size=0},Nn.prototype.delete=function(n){var t=this.__data__;return n=st(t,n),!(0>n)&&(n==t.length-1?t.pop():ji.call(t,n,1),
--this.size,true)},Nn.prototype.get=function(n){var t=this.__data__;return n=st(t,n),0>n?F:t[n][1]},Nn.prototype.has=function(n){return-1<st(this.__data__,n)},Nn.prototype.set=function(n,t){var r=this.__data__,e=st(r,n);return 0>e?(++this.size,r.push([n,t])):r[e][1]=t,this},Pn.prototype.clear=function(){this.size=0,this.__data__={hash:new Tn,map:new(Pi||Nn),string:new Tn}},Pn.prototype.delete=function(n){return n=xe(this,n).delete(n),this.size-=n?1:0,n},Pn.prototype.get=function(n){return xe(this,n).get(n);
},Pn.prototype.has=function(n){return xe(this,n).has(n)},Pn.prototype.set=function(n,t){var r=xe(this,n),e=r.size;return r.set(n,t),this.size+=r.size==e?0:1,this},qn.prototype.add=qn.prototype.push=function(n){return this.__data__.set(n,"__lodash_hash_undefined__"),this},qn.prototype.has=function(n){return this.__data__.has(n)},Vn.prototype.clear=function(){this.__data__=new Nn,this.size=0},Vn.prototype.delete=function(n){var t=this.__data__;return n=t.delete(n),this.size=t.size,n},Vn.prototype.get=function(n){
return this.__data__.get(n)},Vn.prototype.has=function(n){return this.__data__.has(n)},Vn.prototype.set=function(n,t){var r=this.__data__;if(r instanceof Nn){var e=r.__data__;if(!Pi||199>e.length)return e.push([n,t]),this.size=++r.size,this;r=this.__data__=new Pn(e)}return r.set(n,t),this.size=r.size,this};var io=qr(Ot),oo=qr(St,true),fo=Vr(),co=Vr(true),ao=Gi?function(n,t){return Gi.set(n,t),n}:Fu,lo=ki?function(n,t){return ki(n,"toString",{configurable:true,enumerable:false,value:$u(t),writable:true})}:Fu,so=Ei||function(n){
return Zn.clearTimeout(n)},ho=qi&&1/D(new qi([,-0]))[1]==N?function(n){return new qi(n)}:Zu,po=Gi?function(n){return Gi.get(n)}:Zu,_o=zi?U(zi,Xu):Vu,vo=zi?function(n){for(var t=[];n;)s(t,_o(n)),n=yi(n);return t}:Vu,go=Wt;(Ni&&"[object DataView]"!=go(new Ni(new ArrayBuffer(1)))||Pi&&"[object Map]"!=go(new Pi)||Zi&&"[object Promise]"!=go(Zi.resolve())||qi&&"[object Set]"!=go(new qi)||Vi&&"[object WeakMap]"!=go(new Vi))&&(go=function(n){var t=Wt(n);if(n=(n="[object Object]"==t?n.constructor:F)?$e(n):"")switch(n){
case Ji:return"[object DataView]";case Yi:return"[object Map]";case Qi:return"[object Promise]";case Xi:return"[object Set]";case no:return"[object WeakMap]"}return t});var yo=ii?vu:Ku,bo=De(ao),xo=Si||function(n,t){return Zn.setTimeout(n,t)},jo=De(lo),wo=function(n){n=au(n,function(n){return 500===t.size&&t.clear(),n});var t=n.cache;return n}(function(n){var t=[];return en.test(n)&&t.push(""),n.replace(un,function(n,r,e,u){t.push(e?u.replace(vn,"$1"):r||n)}),t}),mo=sr(function(n,t){return pu(n)?wt(n,Et(t,1,pu,true)):[];
}),Ao=sr(function(n,t){var r=Ke(t);return pu(r)&&(r=F),pu(n)?wt(n,Et(t,1,pu,true),be(r,2)):[]}),ko=sr(function(n,t){var r=Ke(t);return pu(r)&&(r=F),pu(n)?wt(n,Et(t,1,pu,true),F,r):[]}),Eo=sr(function(n){var t=l(n,Ir);return t.length&&t[0]===n[0]?Ct(t):[]}),Oo=sr(function(n){var t=Ke(n),r=l(n,Ir);return t===Ke(r)?t=F:r.pop(),r.length&&r[0]===n[0]?Ct(r,be(t,2)):[]}),So=sr(function(n){var t=Ke(n),r=l(n,Ir);return(t=typeof t=="function"?t:F)&&r.pop(),r.length&&r[0]===n[0]?Ct(r,F,t):[]}),Io=sr(Ge),Ro=_e(function(n,t){
var r=null==n?0:n.length,e=gt(n,t);return cr(n,l(t,function(n){return Se(n,r)?+n:n}).sort(Cr)),e}),zo=sr(function(n){return mr(Et(n,1,pu,true))}),Wo=sr(function(n){var t=Ke(n);return pu(t)&&(t=F),mr(Et(n,1,pu,true),be(t,2))}),Bo=sr(function(n){var t=Ke(n),t=typeof t=="function"?t:F;return mr(Et(n,1,pu,true),F,t)}),Lo=sr(function(n,t){return pu(n)?wt(n,t):[]}),Uo=sr(function(n){return Or(f(n,pu))}),Co=sr(function(n){var t=Ke(n);return pu(t)&&(t=F),Or(f(n,pu),be(t,2))}),Do=sr(function(n){var t=Ke(n),t=typeof t=="function"?t:F;
return Or(f(n,pu),F,t)}),Mo=sr(Je),To=sr(function(n){var t=n.length,t=1<t?n[t-1]:F,t=typeof t=="function"?(n.pop(),t):F;return Ye(n,t)}),$o=_e(function(n){function t(t){return gt(t,n)}var r=n.length,e=r?n[0]:0,u=this.__wrapped__;return!(1<r||this.__actions__.length)&&u instanceof Mn&&Se(e)?(u=u.slice(e,+e+(r?1:0)),u.__actions__.push({func:Xe,args:[t],thisArg:F}),new zn(u,this.__chain__).thru(function(n){return r&&!n.length&&n.push(F),n})):this.thru(t)}),Fo=Pr(function(n,t,r){fi.call(n,r)?++n[r]:vt(n,r,1);
}),No=Qr(Pe),Po=Qr(Ze),Zo=Pr(function(n,t,r){fi.call(n,r)?n[r].push(t):vt(n,r,[t])}),qo=sr(function(n,t,e){var u=-1,i=typeof t=="function",o=hu(n)?Gu(n.length):[];return io(n,function(n){o[++u]=i?r(t,n,e):Mt(n,t,e)}),o}),Vo=Pr(function(n,t,r){vt(n,r,t)}),Ko=Pr(function(n,t,r){n[r?0:1].push(t)},function(){return[[],[]]}),Go=sr(function(n,t){if(null==n)return[];var r=t.length;return 1<r&&Ie(n,t[0],t[1])?t=[]:2<r&&Ie(t[0],t[1],t[2])&&(t=[t[0]]),er(n,Et(t,1),[])}),Ho=Oi||function(){return Zn.Date.now();
},Jo=sr(function(n,t,r){var e=1;if(r.length)var u=C(r,ye(Jo)),e=32|e;return se(n,e,t,r,u)}),Yo=sr(function(n,t,r){var e=3;if(r.length)var u=C(r,ye(Yo)),e=32|e;return se(t,e,n,r,u)}),Qo=sr(function(n,t){return jt(n,1,t)}),Xo=sr(function(n,t,r){return jt(n,Su(t)||0,r)});au.Cache=Pn;var nf=sr(function(n,t){t=1==t.length&&cf(t[0])?l(t[0],S(be())):l(Et(t,1),S(be()));var e=t.length;return sr(function(u){for(var i=-1,o=Di(u.length,e);++i<o;)u[i]=t[i].call(this,u[i]);return r(n,this,u)})}),tf=sr(function(n,t){
return se(n,32,F,t,C(t,ye(tf)))}),rf=sr(function(n,t){return se(n,64,F,t,C(t,ye(rf)))}),ef=_e(function(n,t){return se(n,256,F,F,F,t)}),uf=fe(Bt),of=fe(function(n,t){return n>=t}),ff=Tt(function(){return arguments}())?Tt:function(n){return bu(n)&&fi.call(n,"callee")&&!xi.call(n,"callee")},cf=Gu.isArray,af=Hn?S(Hn):$t,lf=Wi||Ku,sf=Jn?S(Jn):Ft,hf=Yn?S(Yn):Pt,pf=Qn?S(Qn):Vt,_f=Xn?S(Xn):Kt,vf=nt?S(nt):Gt,gf=fe(Yt),df=fe(function(n,t){return n<=t}),yf=Zr(function(n,t){if(We(t)||hu(t))$r(t,Bu(t),n);else for(var r in t)fi.call(t,r)&&lt(n,r,t[r]);
}),bf=Zr(function(n,t){$r(t,Lu(t),n)}),xf=Zr(function(n,t,r,e){$r(t,Lu(t),n,e)}),jf=Zr(function(n,t,r,e){$r(t,Bu(t),n,e)}),wf=_e(gt),mf=sr(function(n){return n.push(F,ct),r(xf,F,n)}),Af=sr(function(n){return n.push(F,Le),r(If,F,n)}),kf=te(function(n,t,r){n[t]=r},$u(Fu)),Ef=te(function(n,t,r){fi.call(n,t)?n[t].push(r):n[t]=[r]},be),Of=sr(Mt),Sf=Zr(function(n,t,r){tr(n,t,r)}),If=Zr(function(n,t,r,e){tr(n,t,r,e)}),Rf=_e(function(n,t){var r={};if(null==n)return r;var e=false;t=l(t,function(t){return t=zr(t,n),
e||(e=1<t.length),t}),$r(n,ge(n),r),e&&(r=yt(r,7));for(var u=t.length;u--;)Ar(r,t[u]);return r}),zf=_e(function(n,t){return null==n?{}:ur(n,t)}),Wf=le(Bu),Bf=le(Lu),Lf=Hr(function(n,t,r){return t=t.toLowerCase(),n+(r?Du(t):t)}),Uf=Hr(function(n,t,r){return n+(r?"-":"")+t.toLowerCase()}),Cf=Hr(function(n,t,r){return n+(r?" ":"")+t.toLowerCase()}),Df=Gr("toLowerCase"),Mf=Hr(function(n,t,r){return n+(r?"_":"")+t.toLowerCase()}),Tf=Hr(function(n,t,r){return n+(r?" ":"")+Ff(t)}),$f=Hr(function(n,t,r){
return n+(r?" ":"")+t.toUpperCase()}),Ff=Gr("toUpperCase"),Nf=sr(function(n,t){try{return r(n,F,t)}catch(n){return _u(n)?n:new Ju(n)}}),Pf=_e(function(n,t){return u(t,function(t){t=Te(t),vt(n,t,Jo(n[t],n))}),n}),Zf=Xr(),qf=Xr(true),Vf=sr(function(n,t){return function(r){return Mt(r,n,t)}}),Kf=sr(function(n,t){return function(r){return Mt(n,r,t)}}),Gf=ee(l),Hf=ee(o),Jf=ee(_),Yf=oe(),Qf=oe(true),Xf=re(function(n,t){return n+t},0),nc=ae("ceil"),tc=re(function(n,t){return n/t},1),rc=ae("floor"),ec=re(function(n,t){
return n*t},1),uc=ae("round"),ic=re(function(n,t){return n-t},0);return On.after=function(n,t){if(typeof t!="function")throw new ri("Expected a function");return n=Eu(n),function(){if(1>--n)return t.apply(this,arguments)}},On.ary=uu,On.assign=yf,On.assignIn=bf,On.assignInWith=xf,On.assignWith=jf,On.at=wf,On.before=iu,On.bind=Jo,On.bindAll=Pf,On.bindKey=Yo,On.castArray=function(){if(!arguments.length)return[];var n=arguments[0];return cf(n)?n:[n]},On.chain=Qe,On.chunk=function(n,t,r){if(t=(r?Ie(n,t,r):t===F)?1:Ci(Eu(t),0),
r=null==n?0:n.length,!r||1>t)return[];for(var e=0,u=0,i=Gu(Ii(r/t));e<r;)i[u++]=gr(n,e,e+=t);return i},On.compact=function(n){for(var t=-1,r=null==n?0:n.length,e=0,u=[];++t<r;){var i=n[t];i&&(u[e++]=i)}return u},On.concat=function(){var n=arguments.length;if(!n)return[];for(var t=Gu(n-1),r=arguments[0];n--;)t[n-1]=arguments[n];return s(cf(r)?Tr(r):[r],Et(t,1))},On.cond=function(n){var t=null==n?0:n.length,e=be();return n=t?l(n,function(n){if("function"!=typeof n[1])throw new ri("Expected a function");
return[e(n[0]),n[1]]}):[],sr(function(e){for(var u=-1;++u<t;){var i=n[u];if(r(i[0],this,e))return r(i[1],this,e)}})},On.conforms=function(n){return bt(yt(n,1))},On.constant=$u,On.countBy=Fo,On.create=function(n,t){var r=uo(n);return null==t?r:pt(r,t)},On.curry=ou,On.curryRight=fu,On.debounce=cu,On.defaults=mf,On.defaultsDeep=Af,On.defer=Qo,On.delay=Xo,On.difference=mo,On.differenceBy=Ao,On.differenceWith=ko,On.drop=function(n,t,r){var e=null==n?0:n.length;return e?(t=r||t===F?1:Eu(t),gr(n,0>t?0:t,e)):[];
},On.dropRight=function(n,t,r){var e=null==n?0:n.length;return e?(t=r||t===F?1:Eu(t),t=e-t,gr(n,0,0>t?0:t)):[]},On.dropRightWhile=function(n,t){return n&&n.length?kr(n,be(t,3),true,true):[]},On.dropWhile=function(n,t){return n&&n.length?kr(n,be(t,3),true):[]},On.fill=function(n,t,r,e){var u=null==n?0:n.length;if(!u)return[];for(r&&typeof r!="number"&&Ie(n,t,r)&&(r=0,e=u),u=n.length,r=Eu(r),0>r&&(r=-r>u?0:u+r),e=e===F||e>u?u:Eu(e),0>e&&(e+=u),e=r>e?0:Ou(e);r<e;)n[r++]=t;return n},On.filter=function(n,t){
return(cf(n)?f:kt)(n,be(t,3))},On.flatMap=function(n,t){return Et(eu(n,t),1)},On.flatMapDeep=function(n,t){return Et(eu(n,t),N)},On.flatMapDepth=function(n,t,r){return r=r===F?1:Eu(r),Et(eu(n,t),r)},On.flatten=qe,On.flattenDeep=function(n){return(null==n?0:n.length)?Et(n,N):[]},On.flattenDepth=function(n,t){return null!=n&&n.length?(t=t===F?1:Eu(t),Et(n,t)):[]},On.flip=function(n){return se(n,512)},On.flow=Zf,On.flowRight=qf,On.fromPairs=function(n){for(var t=-1,r=null==n?0:n.length,e={};++t<r;){
var u=n[t];e[u[0]]=u[1]}return e},On.functions=function(n){return null==n?[]:It(n,Bu(n))},On.functionsIn=function(n){return null==n?[]:It(n,Lu(n))},On.groupBy=Zo,On.initial=function(n){return(null==n?0:n.length)?gr(n,0,-1):[]},On.intersection=Eo,On.intersectionBy=Oo,On.intersectionWith=So,On.invert=kf,On.invertBy=Ef,On.invokeMap=qo,On.iteratee=Nu,On.keyBy=Vo,On.keys=Bu,On.keysIn=Lu,On.map=eu,On.mapKeys=function(n,t){var r={};return t=be(t,3),Ot(n,function(n,e,u){vt(r,t(n,e,u),n)}),r},On.mapValues=function(n,t){
var r={};return t=be(t,3),Ot(n,function(n,e,u){vt(r,e,t(n,e,u))}),r},On.matches=function(n){return Xt(yt(n,1))},On.matchesProperty=function(n,t){return nr(n,yt(t,1))},On.memoize=au,On.merge=Sf,On.mergeWith=If,On.method=Vf,On.methodOf=Kf,On.mixin=Pu,On.negate=lu,On.nthArg=function(n){return n=Eu(n),sr(function(t){return rr(t,n)})},On.omit=Rf,On.omitBy=function(n,t){return Uu(n,lu(be(t)))},On.once=function(n){return iu(2,n)},On.orderBy=function(n,t,r,e){return null==n?[]:(cf(t)||(t=null==t?[]:[t]),
r=e?F:r,cf(r)||(r=null==r?[]:[r]),er(n,t,r))},On.over=Gf,On.overArgs=nf,On.overEvery=Hf,On.overSome=Jf,On.partial=tf,On.partialRight=rf,On.partition=Ko,On.pick=zf,On.pickBy=Uu,On.property=qu,On.propertyOf=function(n){return function(t){return null==n?F:Rt(n,t)}},On.pull=Io,On.pullAll=Ge,On.pullAllBy=function(n,t,r){return n&&n.length&&t&&t.length?fr(n,t,be(r,2)):n},On.pullAllWith=function(n,t,r){return n&&n.length&&t&&t.length?fr(n,t,F,r):n},On.pullAt=Ro,On.range=Yf,On.rangeRight=Qf,On.rearg=ef,On.reject=function(n,t){
return(cf(n)?f:kt)(n,lu(be(t,3)))},On.remove=function(n,t){var r=[];if(!n||!n.length)return r;var e=-1,u=[],i=n.length;for(t=be(t,3);++e<i;){var o=n[e];t(o,e,n)&&(r.push(o),u.push(e))}return cr(n,u),r},On.rest=function(n,t){if(typeof n!="function")throw new ri("Expected a function");return t=t===F?t:Eu(t),sr(n,t)},On.reverse=He,On.sampleSize=function(n,t,r){return t=(r?Ie(n,t,r):t===F)?1:Eu(t),(cf(n)?ot:pr)(n,t)},On.set=function(n,t,r){return null==n?n:_r(n,t,r)},On.setWith=function(n,t,r,e){return e=typeof e=="function"?e:F,
null==n?n:_r(n,t,r,e)},On.shuffle=function(n){return(cf(n)?ft:vr)(n)},On.slice=function(n,t,r){var e=null==n?0:n.length;return e?(r&&typeof r!="number"&&Ie(n,t,r)?(t=0,r=e):(t=null==t?0:Eu(t),r=r===F?e:Eu(r)),gr(n,t,r)):[]},On.sortBy=Go,On.sortedUniq=function(n){return n&&n.length?xr(n):[]},On.sortedUniqBy=function(n,t){return n&&n.length?xr(n,be(t,2)):[]},On.split=function(n,t,r){return r&&typeof r!="number"&&Ie(n,t,r)&&(t=r=F),r=r===F?4294967295:r>>>0,r?(n=Ru(n))&&(typeof t=="string"||null!=t&&!pf(t))&&(t=wr(t),
!t&&Bn.test(n))?Wr($(n),0,r):n.split(t,r):[]},On.spread=function(n,t){if(typeof n!="function")throw new ri("Expected a function");return t=t===F?0:Ci(Eu(t),0),sr(function(e){var u=e[t];return e=Wr(e,0,t),u&&s(e,u),r(n,this,e)})},On.tail=function(n){var t=null==n?0:n.length;return t?gr(n,1,t):[]},On.take=function(n,t,r){return n&&n.length?(t=r||t===F?1:Eu(t),gr(n,0,0>t?0:t)):[]},On.takeRight=function(n,t,r){var e=null==n?0:n.length;return e?(t=r||t===F?1:Eu(t),t=e-t,gr(n,0>t?0:t,e)):[]},On.takeRightWhile=function(n,t){
return n&&n.length?kr(n,be(t,3),false,true):[]},On.takeWhile=function(n,t){return n&&n.length?kr(n,be(t,3)):[]},On.tap=function(n,t){return t(n),n},On.throttle=function(n,t,r){var e=true,u=true;if(typeof n!="function")throw new ri("Expected a function");return yu(r)&&(e="leading"in r?!!r.leading:e,u="trailing"in r?!!r.trailing:u),cu(n,t,{leading:e,maxWait:t,trailing:u})},On.thru=Xe,On.toArray=Au,On.toPairs=Wf,On.toPairsIn=Bf,On.toPath=function(n){return cf(n)?l(n,Te):mu(n)?[n]:Tr(wo(Ru(n)))},On.toPlainObject=Iu,
On.transform=function(n,t,r){var e=cf(n),i=e||lf(n)||vf(n);if(t=be(t,4),null==r){var o=n&&n.constructor;r=i?e?new o:[]:yu(n)&&vu(o)?uo(yi(n)):{}}return(i?u:Ot)(n,function(n,e,u){return t(r,n,e,u)}),r},On.unary=function(n){return uu(n,1)},On.union=zo,On.unionBy=Wo,On.unionWith=Bo,On.uniq=function(n){return n&&n.length?mr(n):[]},On.uniqBy=function(n,t){return n&&n.length?mr(n,be(t,2)):[]},On.uniqWith=function(n,t){return t=typeof t=="function"?t:F,n&&n.length?mr(n,F,t):[]},On.unset=function(n,t){return null==n||Ar(n,t);
},On.unzip=Je,On.unzipWith=Ye,On.update=function(n,t,r){return null==n?n:_r(n,t,Rr(r)(Rt(n,t)),void 0)},On.updateWith=function(n,t,r,e){return e=typeof e=="function"?e:F,null!=n&&(n=_r(n,t,Rr(r)(Rt(n,t)),e)),n},On.values=Cu,On.valuesIn=function(n){return null==n?[]:I(n,Lu(n))},On.without=Lo,On.words=Tu,On.wrap=function(n,t){return tf(Rr(t),n)},On.xor=Uo,On.xorBy=Co,On.xorWith=Do,On.zip=Mo,On.zipObject=function(n,t){return Sr(n||[],t||[],lt)},On.zipObjectDeep=function(n,t){return Sr(n||[],t||[],_r);
},On.zipWith=To,On.entries=Wf,On.entriesIn=Bf,On.extend=bf,On.extendWith=xf,Pu(On,On),On.add=Xf,On.attempt=Nf,On.camelCase=Lf,On.capitalize=Du,On.ceil=nc,On.clamp=function(n,t,r){return r===F&&(r=t,t=F),r!==F&&(r=Su(r),r=r===r?r:0),t!==F&&(t=Su(t),t=t===t?t:0),dt(Su(n),t,r)},On.clone=function(n){return yt(n,4)},On.cloneDeep=function(n){return yt(n,5)},On.cloneDeepWith=function(n,t){return t=typeof t=="function"?t:F,yt(n,5,t)},On.cloneWith=function(n,t){return t=typeof t=="function"?t:F,yt(n,4,t)},
On.conformsTo=function(n,t){return null==t||xt(n,t,Bu(t))},On.deburr=Mu,On.defaultTo=function(n,t){return null==n||n!==n?t:n},On.divide=tc,On.endsWith=function(n,t,r){n=Ru(n),t=wr(t);var e=n.length,e=r=r===F?e:dt(Eu(r),0,e);return r-=t.length,0<=r&&n.slice(r,e)==t},On.eq=su,On.escape=function(n){return(n=Ru(n))&&Y.test(n)?n.replace(H,et):n},On.escapeRegExp=function(n){return(n=Ru(n))&&fn.test(n)?n.replace(on,"\\$&"):n},On.every=function(n,t,r){var e=cf(n)?o:mt;return r&&Ie(n,t,r)&&(t=F),e(n,be(t,3));
},On.find=No,On.findIndex=Pe,On.findKey=function(n,t){return v(n,be(t,3),Ot)},On.findLast=Po,On.findLastIndex=Ze,On.findLastKey=function(n,t){return v(n,be(t,3),St)},On.floor=rc,On.forEach=tu,On.forEachRight=ru,On.forIn=function(n,t){return null==n?n:fo(n,be(t,3),Lu)},On.forInRight=function(n,t){return null==n?n:co(n,be(t,3),Lu)},On.forOwn=function(n,t){return n&&Ot(n,be(t,3))},On.forOwnRight=function(n,t){return n&&St(n,be(t,3))},On.get=zu,On.gt=uf,On.gte=of,On.has=function(n,t){return null!=n&&me(n,t,Lt);
},On.hasIn=Wu,On.head=Ve,On.identity=Fu,On.includes=function(n,t,r,e){return n=hu(n)?n:Cu(n),r=r&&!e?Eu(r):0,e=n.length,0>r&&(r=Ci(e+r,0)),wu(n)?r<=e&&-1<n.indexOf(t,r):!!e&&-1<d(n,t,r)},On.indexOf=function(n,t,r){var e=null==n?0:n.length;return e?(r=null==r?0:Eu(r),0>r&&(r=Ci(e+r,0)),d(n,t,r)):-1},On.inRange=function(n,t,r){return t=ku(t),r===F?(r=t,t=0):r=ku(r),n=Su(n),n>=Di(t,r)&&n<Ci(t,r)},On.invoke=Of,On.isArguments=ff,On.isArray=cf,On.isArrayBuffer=af,On.isArrayLike=hu,On.isArrayLikeObject=pu,
On.isBoolean=function(n){return true===n||false===n||bu(n)&&"[object Boolean]"==Wt(n)},On.isBuffer=lf,On.isDate=sf,On.isElement=function(n){return bu(n)&&1===n.nodeType&&!ju(n)},On.isEmpty=function(n){if(null==n)return true;if(hu(n)&&(cf(n)||typeof n=="string"||typeof n.splice=="function"||lf(n)||vf(n)||ff(n)))return!n.length;var t=go(n);if("[object Map]"==t||"[object Set]"==t)return!n.size;if(We(n))return!Jt(n).length;for(var r in n)if(fi.call(n,r))return false;return true},On.isEqual=function(n,t){return Nt(n,t);
},On.isEqualWith=function(n,t,r){var e=(r=typeof r=="function"?r:F)?r(n,t):F;return e===F?Nt(n,t,F,r):!!e},On.isError=_u,On.isFinite=function(n){return typeof n=="number"&&Bi(n)},On.isFunction=vu,On.isInteger=gu,On.isLength=du,On.isMap=hf,On.isMatch=function(n,t){return n===t||Zt(n,t,je(t))},On.isMatchWith=function(n,t,r){return r=typeof r=="function"?r:F,Zt(n,t,je(t),r)},On.isNaN=function(n){return xu(n)&&n!=+n},On.isNative=function(n){if(yo(n))throw new Ju("Unsupported core-js use. Try https://npms.io/search?q=ponyfill.");
return qt(n)},On.isNil=function(n){return null==n},On.isNull=function(n){return null===n},On.isNumber=xu,On.isObject=yu,On.isObjectLike=bu,On.isPlainObject=ju,On.isRegExp=pf,On.isSafeInteger=function(n){return gu(n)&&-9007199254740991<=n&&9007199254740991>=n},On.isSet=_f,On.isString=wu,On.isSymbol=mu,On.isTypedArray=vf,On.isUndefined=function(n){return n===F},On.isWeakMap=function(n){return bu(n)&&"[object WeakMap]"==go(n)},On.isWeakSet=function(n){return bu(n)&&"[object WeakSet]"==Wt(n)},On.join=function(n,t){
return null==n?"":Li.call(n,t)},On.kebabCase=Uf,On.last=Ke,On.lastIndexOf=function(n,t,r){var e=null==n?0:n.length;if(!e)return-1;var u=e;if(r!==F&&(u=Eu(r),u=0>u?Ci(e+u,0):Di(u,e-1)),t===t){for(r=u+1;r--&&n[r]!==t;);n=r}else n=g(n,b,u,true);return n},On.lowerCase=Cf,On.lowerFirst=Df,On.lt=gf,On.lte=df,On.max=function(n){return n&&n.length?At(n,Fu,Bt):F},On.maxBy=function(n,t){return n&&n.length?At(n,be(t,2),Bt):F},On.mean=function(n){return x(n,Fu)},On.meanBy=function(n,t){return x(n,be(t,2))},On.min=function(n){
return n&&n.length?At(n,Fu,Yt):F},On.minBy=function(n,t){return n&&n.length?At(n,be(t,2),Yt):F},On.stubArray=Vu,On.stubFalse=Ku,On.stubObject=function(){return{}},On.stubString=function(){return""},On.stubTrue=function(){return true},On.multiply=ec,On.nth=function(n,t){return n&&n.length?rr(n,Eu(t)):F},On.noConflict=function(){return Zn._===this&&(Zn._=hi),this},On.noop=Zu,On.now=Ho,On.pad=function(n,t,r){n=Ru(n);var e=(t=Eu(t))?T(n):0;return!t||e>=t?n:(t=(t-e)/2,ue(Ri(t),r)+n+ue(Ii(t),r))},On.padEnd=function(n,t,r){
n=Ru(n);var e=(t=Eu(t))?T(n):0;return t&&e<t?n+ue(t-e,r):n},On.padStart=function(n,t,r){n=Ru(n);var e=(t=Eu(t))?T(n):0;return t&&e<t?ue(t-e,r)+n:n},On.parseInt=function(n,t,r){return r||null==t?t=0:t&&(t=+t),Ti(Ru(n).replace(an,""),t||0)},On.random=function(n,t,r){if(r&&typeof r!="boolean"&&Ie(n,t,r)&&(t=r=F),r===F&&(typeof t=="boolean"?(r=t,t=F):typeof n=="boolean"&&(r=n,n=F)),n===F&&t===F?(n=0,t=1):(n=ku(n),t===F?(t=n,n=0):t=ku(t)),n>t){var e=n;n=t,t=e}return r||n%1||t%1?(r=$i(),Di(n+r*(t-n+$n("1e-"+((r+"").length-1))),t)):ar(n,t);
},On.reduce=function(n,t,r){var e=cf(n)?h:m,u=3>arguments.length;return e(n,be(t,4),r,u,io)},On.reduceRight=function(n,t,r){var e=cf(n)?p:m,u=3>arguments.length;return e(n,be(t,4),r,u,oo)},On.repeat=function(n,t,r){return t=(r?Ie(n,t,r):t===F)?1:Eu(t),lr(Ru(n),t)},On.replace=function(){var n=arguments,t=Ru(n[0]);return 3>n.length?t:t.replace(n[1],n[2])},On.result=function(n,t,r){t=zr(t,n);var e=-1,u=t.length;for(u||(u=1,n=F);++e<u;){var i=null==n?F:n[Te(t[e])];i===F&&(e=u,i=r),n=vu(i)?i.call(n):i;
}return n},On.round=uc,On.runInContext=w,On.sample=function(n){return(cf(n)?tt:hr)(n)},On.size=function(n){if(null==n)return 0;if(hu(n))return wu(n)?T(n):n.length;var t=go(n);return"[object Map]"==t||"[object Set]"==t?n.size:Jt(n).length},On.snakeCase=Mf,On.some=function(n,t,r){var e=cf(n)?_:dr;return r&&Ie(n,t,r)&&(t=F),e(n,be(t,3))},On.sortedIndex=function(n,t){return yr(n,t)},On.sortedIndexBy=function(n,t,r){return br(n,t,be(r,2))},On.sortedIndexOf=function(n,t){var r=null==n?0:n.length;if(r){
var e=yr(n,t);if(e<r&&su(n[e],t))return e}return-1},On.sortedLastIndex=function(n,t){return yr(n,t,true)},On.sortedLastIndexBy=function(n,t,r){return br(n,t,be(r,2),true)},On.sortedLastIndexOf=function(n,t){if(null==n?0:n.length){var r=yr(n,t,true)-1;if(su(n[r],t))return r}return-1},On.startCase=Tf,On.startsWith=function(n,t,r){return n=Ru(n),r=dt(Eu(r),0,n.length),t=wr(t),n.slice(r,r+t.length)==t},On.subtract=ic,On.sum=function(n){return n&&n.length?k(n,Fu):0},On.sumBy=function(n,t){return n&&n.length?k(n,be(t,2)):0;
},On.template=function(n,t,r){var e=On.templateSettings;r&&Ie(n,t,r)&&(t=F),n=Ru(n),t=xf({},t,e,ct),r=xf({},t.imports,e.imports,ct);var u,i,o=Bu(r),f=I(r,o),c=0;r=t.interpolate||An;var a="__p+='";r=ni((t.escape||An).source+"|"+r.source+"|"+(r===nn?gn:An).source+"|"+(t.evaluate||An).source+"|$","g");var l="sourceURL"in t?"//# sourceURL="+t.sourceURL+"\n":"";if(n.replace(r,function(t,r,e,o,f,l){return e||(e=o),a+=n.slice(c,l).replace(kn,B),r&&(u=true,a+="'+__e("+r+")+'"),f&&(i=true,a+="';"+f+";\n__p+='"),
e&&(a+="'+((__t=("+e+"))==null?'':__t)+'"),c=l+t.length,t}),a+="';",(t=t.variable)||(a="with(obj){"+a+"}"),a=(i?a.replace(q,""):a).replace(V,"$1").replace(K,"$1;"),a="function("+(t||"obj")+"){"+(t?"":"obj||(obj={});")+"var __t,__p=''"+(u?",__e=_.escape":"")+(i?",__j=Array.prototype.join;function print(){__p+=__j.call(arguments,'')}":";")+a+"return __p}",t=Nf(function(){return Yu(o,l+"return "+a).apply(F,f)}),t.source=a,_u(t))throw t;return t},On.times=function(n,t){if(n=Eu(n),1>n||9007199254740991<n)return[];
var r=4294967295,e=Di(n,4294967295);for(t=be(t),n-=4294967295,e=E(e,t);++r<n;)t(r);return e},On.toFinite=ku,On.toInteger=Eu,On.toLength=Ou,On.toLower=function(n){return Ru(n).toLowerCase()},On.toNumber=Su,On.toSafeInteger=function(n){return dt(Eu(n),-9007199254740991,9007199254740991)},On.toString=Ru,On.toUpper=function(n){return Ru(n).toUpperCase()},On.trim=function(n,t,r){return(n=Ru(n))&&(r||t===F)?n.replace(cn,""):n&&(t=wr(t))?(n=$(n),r=$(t),t=z(n,r),r=W(n,r)+1,Wr(n,t,r).join("")):n},On.trimEnd=function(n,t,r){
return(n=Ru(n))&&(r||t===F)?n.replace(ln,""):n&&(t=wr(t))?(n=$(n),t=W(n,$(t))+1,Wr(n,0,t).join("")):n},On.trimStart=function(n,t,r){return(n=Ru(n))&&(r||t===F)?n.replace(an,""):n&&(t=wr(t))?(n=$(n),t=z(n,$(t)),Wr(n,t).join("")):n},On.truncate=function(n,t){var r=30,e="...";if(yu(t))var u="separator"in t?t.separator:u,r="length"in t?Eu(t.length):r,e="omission"in t?wr(t.omission):e;n=Ru(n);var i=n.length;if(Bn.test(n))var o=$(n),i=o.length;if(r>=i)return n;if(i=r-T(e),1>i)return e;if(r=o?Wr(o,0,i).join(""):n.slice(0,i),
u===F)return r+e;if(o&&(i+=r.length-i),pf(u)){if(n.slice(i).search(u)){var f=r;for(u.global||(u=ni(u.source,Ru(dn.exec(u))+"g")),u.lastIndex=0;o=u.exec(f);)var c=o.index;r=r.slice(0,c===F?i:c)}}else n.indexOf(wr(u),i)!=i&&(u=r.lastIndexOf(u),-1<u&&(r=r.slice(0,u)));return r+e},On.unescape=function(n){return(n=Ru(n))&&J.test(n)?n.replace(G,ut):n},On.uniqueId=function(n){var t=++ci;return Ru(n)+t},On.upperCase=$f,On.upperFirst=Ff,On.each=tu,On.eachRight=ru,On.first=Ve,Pu(On,function(){var n={};return Ot(On,function(t,r){
fi.call(On.prototype,r)||(n[r]=t)}),n}(),{chain:false}),On.VERSION="4.17.2",u("bind bindKey curry curryRight partial partialRight".split(" "),function(n){On[n].placeholder=On}),u(["drop","take"],function(n,t){Mn.prototype[n]=function(r){var e=this.__filtered__;if(e&&!t)return new Mn(this);r=r===F?1:Ci(Eu(r),0);var u=this.clone();return e?u.__takeCount__=Di(r,u.__takeCount__):u.__views__.push({size:Di(r,4294967295),type:n+(0>u.__dir__?"Right":"")}),u},Mn.prototype[n+"Right"]=function(t){return this.reverse()[n](t).reverse();
}}),u(["filter","map","takeWhile"],function(n,t){var r=t+1,e=1==r||3==r;Mn.prototype[n]=function(n){var t=this.clone();return t.__iteratees__.push({iteratee:be(n,3),type:r}),t.__filtered__=t.__filtered__||e,t}}),u(["head","last"],function(n,t){var r="take"+(t?"Right":"");Mn.prototype[n]=function(){return this[r](1).value()[0]}}),u(["initial","tail"],function(n,t){var r="drop"+(t?"":"Right");Mn.prototype[n]=function(){return this.__filtered__?new Mn(this):this[r](1)}}),Mn.prototype.compact=function(){
return this.filter(Fu)},Mn.prototype.find=function(n){return this.filter(n).head()},Mn.prototype.findLast=function(n){return this.reverse().find(n)},Mn.prototype.invokeMap=sr(function(n,t){return typeof n=="function"?new Mn(this):this.map(function(r){return Mt(r,n,t)})}),Mn.prototype.reject=function(n){return this.filter(lu(be(n)))},Mn.prototype.slice=function(n,t){n=Eu(n);var r=this;return r.__filtered__&&(0<n||0>t)?new Mn(r):(0>n?r=r.takeRight(-n):n&&(r=r.drop(n)),t!==F&&(t=Eu(t),r=0>t?r.dropRight(-t):r.take(t-n)),
r)},Mn.prototype.takeRightWhile=function(n){return this.reverse().takeWhile(n).reverse()},Mn.prototype.toArray=function(){return this.take(4294967295)},Ot(Mn.prototype,function(n,t){var r=/^(?:filter|find|map|reject)|While$/.test(t),e=/^(?:head|last)$/.test(t),u=On[e?"take"+("last"==t?"Right":""):t],i=e||/^find/.test(t);u&&(On.prototype[t]=function(){function t(n){return n=u.apply(On,s([n],f)),e&&h?n[0]:n}var o=this.__wrapped__,f=e?[1]:arguments,c=o instanceof Mn,a=f[0],l=c||cf(o);l&&r&&typeof a=="function"&&1!=a.length&&(c=l=false);
var h=this.__chain__,p=!!this.__actions__.length,a=i&&!h,c=c&&!p;return!i&&l?(o=c?o:new Mn(this),o=n.apply(o,f),o.__actions__.push({func:Xe,args:[t],thisArg:F}),new zn(o,h)):a&&c?n.apply(this,f):(o=this.thru(t),a?e?o.value()[0]:o.value():o)})}),u("pop push shift sort splice unshift".split(" "),function(n){var t=ei[n],r=/^(?:push|sort|unshift)$/.test(n)?"tap":"thru",e=/^(?:pop|shift)$/.test(n);On.prototype[n]=function(){var n=arguments;if(e&&!this.__chain__){var u=this.value();return t.apply(cf(u)?u:[],n);
}return this[r](function(r){return t.apply(cf(r)?r:[],n)})}}),Ot(Mn.prototype,function(n,t){var r=On[t];if(r){var e=r.name+"";(Hi[e]||(Hi[e]=[])).push({name:t,func:r})}}),Hi[ne(F,2).name]=[{name:"wrapper",func:F}],Mn.prototype.clone=function(){var n=new Mn(this.__wrapped__);return n.__actions__=Tr(this.__actions__),n.__dir__=this.__dir__,n.__filtered__=this.__filtered__,n.__iteratees__=Tr(this.__iteratees__),n.__takeCount__=this.__takeCount__,n.__views__=Tr(this.__views__),n},Mn.prototype.reverse=function(){
if(this.__filtered__){var n=new Mn(this);n.__dir__=-1,n.__filtered__=true}else n=this.clone(),n.__dir__*=-1;return n},Mn.prototype.value=function(){var n,t=this.__wrapped__.value(),r=this.__dir__,e=cf(t),u=0>r,i=e?t.length:0;n=i;for(var o=this.__views__,f=0,c=-1,a=o.length;++c<a;){var l=o[c],s=l.size;switch(l.type){case"drop":f+=s;break;case"dropRight":n-=s;break;case"take":n=Di(n,f+s);break;case"takeRight":f=Ci(f,n-s)}}if(n={start:f,end:n},o=n.start,f=n.end,n=f-o,u=u?f:o-1,o=this.__iteratees__,f=o.length,
c=0,a=Di(n,this.__takeCount__),!e||200>i||i==n&&a==n)return Er(t,this.__actions__);e=[];n:for(;n--&&c<a;){for(u+=r,i=-1,l=t[u];++i<f;){var h=o[i],s=h.type,h=(0,h.iteratee)(l);if(2==s)l=h;else if(!h){if(1==s)continue n;break n}}e[c++]=l}return e},On.prototype.at=$o,On.prototype.chain=function(){return Qe(this)},On.prototype.commit=function(){return new zn(this.value(),this.__chain__)},On.prototype.next=function(){this.__values__===F&&(this.__values__=Au(this.value()));var n=this.__index__>=this.__values__.length;
return{done:n,value:n?F:this.__values__[this.__index__++]}},On.prototype.plant=function(n){for(var t,r=this;r instanceof Sn;){var e=Ne(r);e.__index__=0,e.__values__=F,t?u.__wrapped__=e:t=e;var u=e,r=r.__wrapped__}return u.__wrapped__=n,t},On.prototype.reverse=function(){var n=this.__wrapped__;return n instanceof Mn?(this.__actions__.length&&(n=new Mn(this)),n=n.reverse(),n.__actions__.push({func:Xe,args:[He],thisArg:F}),new zn(n,this.__chain__)):this.thru(He)},On.prototype.toJSON=On.prototype.valueOf=On.prototype.value=function(){
return Er(this.__wrapped__,this.__actions__)},On.prototype.first=On.prototype.head,mi&&(On.prototype[mi]=nu),On}();typeof define=="function"&&typeof define.amd=="object"&&define.amd?(Zn._=it, define(function(){return it})):Vn?((Vn.exports=it)._=it,qn._=it):Zn._=it}).call(this);

//! moment.js
//! version : 2.17.1
//! authors : Tim Wood, Iskren Chernev, Moment.js contributors
//! license : MIT
//! momentjs.com
!function(a,b){"object"==typeof exports&&"undefined"!=typeof module?module.exports=b():"function"==typeof define&&define.amd?define(b):a.moment=b()}(this,function(){"use strict";function a(){return od.apply(null,arguments)}
// This is done to register the method called with moment()
// without creating circular dependencies.
function b(a){od=a}function c(a){return a instanceof Array||"[object Array]"===Object.prototype.toString.call(a)}function d(a){
// IE8 will treat undefined and null as object if it wasn't for
// input != null
return null!=a&&"[object Object]"===Object.prototype.toString.call(a)}function e(a){var b;for(b in a)
// even if its not own property I'd still call it non-empty
return!1;return!0}function f(a){return"number"==typeof a||"[object Number]"===Object.prototype.toString.call(a)}function g(a){return a instanceof Date||"[object Date]"===Object.prototype.toString.call(a)}function h(a,b){var c,d=[];for(c=0;c<a.length;++c)d.push(b(a[c],c));return d}function i(a,b){return Object.prototype.hasOwnProperty.call(a,b)}function j(a,b){for(var c in b)i(b,c)&&(a[c]=b[c]);return i(b,"toString")&&(a.toString=b.toString),i(b,"valueOf")&&(a.valueOf=b.valueOf),a}function k(a,b,c,d){return rb(a,b,c,d,!0).utc()}function l(){
// We need to deep clone this object.
return{empty:!1,unusedTokens:[],unusedInput:[],overflow:-2,charsLeftOver:0,nullInput:!1,invalidMonth:null,invalidFormat:!1,userInvalidated:!1,iso:!1,parsedDateParts:[],meridiem:null}}function m(a){return null==a._pf&&(a._pf=l()),a._pf}function n(a){if(null==a._isValid){var b=m(a),c=qd.call(b.parsedDateParts,function(a){return null!=a}),d=!isNaN(a._d.getTime())&&b.overflow<0&&!b.empty&&!b.invalidMonth&&!b.invalidWeekday&&!b.nullInput&&!b.invalidFormat&&!b.userInvalidated&&(!b.meridiem||b.meridiem&&c);if(a._strict&&(d=d&&0===b.charsLeftOver&&0===b.unusedTokens.length&&void 0===b.bigHour),null!=Object.isFrozen&&Object.isFrozen(a))return d;a._isValid=d}return a._isValid}function o(a){var b=k(NaN);return null!=a?j(m(b),a):m(b).userInvalidated=!0,b}function p(a){return void 0===a}function q(a,b){var c,d,e;if(p(b._isAMomentObject)||(a._isAMomentObject=b._isAMomentObject),p(b._i)||(a._i=b._i),p(b._f)||(a._f=b._f),p(b._l)||(a._l=b._l),p(b._strict)||(a._strict=b._strict),p(b._tzm)||(a._tzm=b._tzm),p(b._isUTC)||(a._isUTC=b._isUTC),p(b._offset)||(a._offset=b._offset),p(b._pf)||(a._pf=m(b)),p(b._locale)||(a._locale=b._locale),rd.length>0)for(c in rd)d=rd[c],e=b[d],p(e)||(a[d]=e);return a}
// Moment prototype object
function r(b){q(this,b),this._d=new Date(null!=b._d?b._d.getTime():NaN),this.isValid()||(this._d=new Date(NaN)),
// Prevent infinite loop in case updateOffset creates new moment
// objects.
sd===!1&&(sd=!0,a.updateOffset(this),sd=!1)}function s(a){return a instanceof r||null!=a&&null!=a._isAMomentObject}function t(a){return a<0?Math.ceil(a)||0:Math.floor(a)}function u(a){var b=+a,c=0;return 0!==b&&isFinite(b)&&(c=t(b)),c}
// compare two arrays, return the number of differences
function v(a,b,c){var d,e=Math.min(a.length,b.length),f=Math.abs(a.length-b.length),g=0;for(d=0;d<e;d++)(c&&a[d]!==b[d]||!c&&u(a[d])!==u(b[d]))&&g++;return g+f}function w(b){a.suppressDeprecationWarnings===!1&&"undefined"!=typeof console&&console.warn&&console.warn("Deprecation warning: "+b)}function x(b,c){var d=!0;return j(function(){if(null!=a.deprecationHandler&&a.deprecationHandler(null,b),d){for(var e,f=[],g=0;g<arguments.length;g++){if(e="","object"==typeof arguments[g]){e+="\n["+g+"] ";for(var h in arguments[0])e+=h+": "+arguments[0][h]+", ";e=e.slice(0,-2)}else e=arguments[g];f.push(e)}w(b+"\nArguments: "+Array.prototype.slice.call(f).join("")+"\n"+(new Error).stack),d=!1}return c.apply(this,arguments)},c)}function y(b,c){null!=a.deprecationHandler&&a.deprecationHandler(b,c),td[b]||(w(c),td[b]=!0)}function z(a){return a instanceof Function||"[object Function]"===Object.prototype.toString.call(a)}function A(a){var b,c;for(c in a)b=a[c],z(b)?this[c]=b:this["_"+c]=b;this._config=a,
// Lenient ordinal parsing accepts just a number in addition to
// number + (possibly) stuff coming from _ordinalParseLenient.
this._ordinalParseLenient=new RegExp(this._ordinalParse.source+"|"+/\d{1,2}/.source)}function B(a,b){var c,e=j({},a);for(c in b)i(b,c)&&(d(a[c])&&d(b[c])?(e[c]={},j(e[c],a[c]),j(e[c],b[c])):null!=b[c]?e[c]=b[c]:delete e[c]);for(c in a)i(a,c)&&!i(b,c)&&d(a[c])&&(
// make sure changes to properties don't modify parent config
e[c]=j({},e[c]));return e}function C(a){null!=a&&this.set(a)}function D(a,b,c){var d=this._calendar[a]||this._calendar.sameElse;return z(d)?d.call(b,c):d}function E(a){var b=this._longDateFormat[a],c=this._longDateFormat[a.toUpperCase()];return b||!c?b:(this._longDateFormat[a]=c.replace(/MMMM|MM|DD|dddd/g,function(a){return a.slice(1)}),this._longDateFormat[a])}function F(){return this._invalidDate}function G(a){return this._ordinal.replace("%d",a)}function H(a,b,c,d){var e=this._relativeTime[c];return z(e)?e(a,b,c,d):e.replace(/%d/i,a)}function I(a,b){var c=this._relativeTime[a>0?"future":"past"];return z(c)?c(b):c.replace(/%s/i,b)}function J(a,b){var c=a.toLowerCase();Dd[c]=Dd[c+"s"]=Dd[b]=a}function K(a){return"string"==typeof a?Dd[a]||Dd[a.toLowerCase()]:void 0}function L(a){var b,c,d={};for(c in a)i(a,c)&&(b=K(c),b&&(d[b]=a[c]));return d}function M(a,b){Ed[a]=b}function N(a){var b=[];for(var c in a)b.push({unit:c,priority:Ed[c]});return b.sort(function(a,b){return a.priority-b.priority}),b}function O(b,c){return function(d){return null!=d?(Q(this,b,d),a.updateOffset(this,c),this):P(this,b)}}function P(a,b){return a.isValid()?a._d["get"+(a._isUTC?"UTC":"")+b]():NaN}function Q(a,b,c){a.isValid()&&a._d["set"+(a._isUTC?"UTC":"")+b](c)}
// MOMENTS
function R(a){return a=K(a),z(this[a])?this[a]():this}function S(a,b){if("object"==typeof a){a=L(a);for(var c=N(a),d=0;d<c.length;d++)this[c[d].unit](a[c[d].unit])}else if(a=K(a),z(this[a]))return this[a](b);return this}function T(a,b,c){var d=""+Math.abs(a),e=b-d.length,f=a>=0;return(f?c?"+":"":"-")+Math.pow(10,Math.max(0,e)).toString().substr(1)+d}
// token:    'M'
// padded:   ['MM', 2]
// ordinal:  'Mo'
// callback: function () { this.month() + 1 }
function U(a,b,c,d){var e=d;"string"==typeof d&&(e=function(){return this[d]()}),a&&(Id[a]=e),b&&(Id[b[0]]=function(){return T(e.apply(this,arguments),b[1],b[2])}),c&&(Id[c]=function(){return this.localeData().ordinal(e.apply(this,arguments),a)})}function V(a){return a.match(/\[[\s\S]/)?a.replace(/^\[|\]$/g,""):a.replace(/\\/g,"")}function W(a){var b,c,d=a.match(Fd);for(b=0,c=d.length;b<c;b++)Id[d[b]]?d[b]=Id[d[b]]:d[b]=V(d[b]);return function(b){var e,f="";for(e=0;e<c;e++)f+=d[e]instanceof Function?d[e].call(b,a):d[e];return f}}
// format date using native date object
function X(a,b){return a.isValid()?(b=Y(b,a.localeData()),Hd[b]=Hd[b]||W(b),Hd[b](a)):a.localeData().invalidDate()}function Y(a,b){function c(a){return b.longDateFormat(a)||a}var d=5;for(Gd.lastIndex=0;d>=0&&Gd.test(a);)a=a.replace(Gd,c),Gd.lastIndex=0,d-=1;return a}function Z(a,b,c){$d[a]=z(b)?b:function(a,d){return a&&c?c:b}}function $(a,b){return i($d,a)?$d[a](b._strict,b._locale):new RegExp(_(a))}
// Code from http://stackoverflow.com/questions/3561493/is-there-a-regexp-escape-function-in-javascript
function _(a){return aa(a.replace("\\","").replace(/\\(\[)|\\(\])|\[([^\]\[]*)\]|\\(.)/g,function(a,b,c,d,e){return b||c||d||e}))}function aa(a){return a.replace(/[-\/\\^$*+?.()|[\]{}]/g,"\\$&")}function ba(a,b){var c,d=b;for("string"==typeof a&&(a=[a]),f(b)&&(d=function(a,c){c[b]=u(a)}),c=0;c<a.length;c++)_d[a[c]]=d}function ca(a,b){ba(a,function(a,c,d,e){d._w=d._w||{},b(a,d._w,d,e)})}function da(a,b,c){null!=b&&i(_d,a)&&_d[a](b,c._a,c,a)}function ea(a,b){return new Date(Date.UTC(a,b+1,0)).getUTCDate()}function fa(a,b){return a?c(this._months)?this._months[a.month()]:this._months[(this._months.isFormat||ke).test(b)?"format":"standalone"][a.month()]:this._months}function ga(a,b){return a?c(this._monthsShort)?this._monthsShort[a.month()]:this._monthsShort[ke.test(b)?"format":"standalone"][a.month()]:this._monthsShort}function ha(a,b,c){var d,e,f,g=a.toLocaleLowerCase();if(!this._monthsParse)for(
// this is not used
this._monthsParse=[],this._longMonthsParse=[],this._shortMonthsParse=[],d=0;d<12;++d)f=k([2e3,d]),this._shortMonthsParse[d]=this.monthsShort(f,"").toLocaleLowerCase(),this._longMonthsParse[d]=this.months(f,"").toLocaleLowerCase();return c?"MMM"===b?(e=je.call(this._shortMonthsParse,g),e!==-1?e:null):(e=je.call(this._longMonthsParse,g),e!==-1?e:null):"MMM"===b?(e=je.call(this._shortMonthsParse,g),e!==-1?e:(e=je.call(this._longMonthsParse,g),e!==-1?e:null)):(e=je.call(this._longMonthsParse,g),e!==-1?e:(e=je.call(this._shortMonthsParse,g),e!==-1?e:null))}function ia(a,b,c){var d,e,f;if(this._monthsParseExact)return ha.call(this,a,b,c);
// TODO: add sorting
// Sorting makes sure if one month (or abbr) is a prefix of another
// see sorting in computeMonthsParse
for(this._monthsParse||(this._monthsParse=[],this._longMonthsParse=[],this._shortMonthsParse=[]),d=0;d<12;d++){
// test the regex
if(
// make the regex if we don't have it already
e=k([2e3,d]),c&&!this._longMonthsParse[d]&&(this._longMonthsParse[d]=new RegExp("^"+this.months(e,"").replace(".","")+"$","i"),this._shortMonthsParse[d]=new RegExp("^"+this.monthsShort(e,"").replace(".","")+"$","i")),c||this._monthsParse[d]||(f="^"+this.months(e,"")+"|^"+this.monthsShort(e,""),this._monthsParse[d]=new RegExp(f.replace(".",""),"i")),c&&"MMMM"===b&&this._longMonthsParse[d].test(a))return d;if(c&&"MMM"===b&&this._shortMonthsParse[d].test(a))return d;if(!c&&this._monthsParse[d].test(a))return d}}
// MOMENTS
function ja(a,b){var c;if(!a.isValid())
// No op
return a;if("string"==typeof b)if(/^\d+$/.test(b))b=u(b);else
// TODO: Another silent failure?
if(b=a.localeData().monthsParse(b),!f(b))return a;return c=Math.min(a.date(),ea(a.year(),b)),a._d["set"+(a._isUTC?"UTC":"")+"Month"](b,c),a}function ka(b){return null!=b?(ja(this,b),a.updateOffset(this,!0),this):P(this,"Month")}function la(){return ea(this.year(),this.month())}function ma(a){return this._monthsParseExact?(i(this,"_monthsRegex")||oa.call(this),a?this._monthsShortStrictRegex:this._monthsShortRegex):(i(this,"_monthsShortRegex")||(this._monthsShortRegex=ne),this._monthsShortStrictRegex&&a?this._monthsShortStrictRegex:this._monthsShortRegex)}function na(a){return this._monthsParseExact?(i(this,"_monthsRegex")||oa.call(this),a?this._monthsStrictRegex:this._monthsRegex):(i(this,"_monthsRegex")||(this._monthsRegex=oe),this._monthsStrictRegex&&a?this._monthsStrictRegex:this._monthsRegex)}function oa(){function a(a,b){return b.length-a.length}var b,c,d=[],e=[],f=[];for(b=0;b<12;b++)
// make the regex if we don't have it already
c=k([2e3,b]),d.push(this.monthsShort(c,"")),e.push(this.months(c,"")),f.push(this.months(c,"")),f.push(this.monthsShort(c,""));for(
// Sorting makes sure if one month (or abbr) is a prefix of another it
// will match the longer piece.
d.sort(a),e.sort(a),f.sort(a),b=0;b<12;b++)d[b]=aa(d[b]),e[b]=aa(e[b]);for(b=0;b<24;b++)f[b]=aa(f[b]);this._monthsRegex=new RegExp("^("+f.join("|")+")","i"),this._monthsShortRegex=this._monthsRegex,this._monthsStrictRegex=new RegExp("^("+e.join("|")+")","i"),this._monthsShortStrictRegex=new RegExp("^("+d.join("|")+")","i")}
// HELPERS
function pa(a){return qa(a)?366:365}function qa(a){return a%4===0&&a%100!==0||a%400===0}function ra(){return qa(this.year())}function sa(a,b,c,d,e,f,g){
//can't just apply() to create a date:
//http://stackoverflow.com/questions/181348/instantiating-a-javascript-object-by-calling-prototype-constructor-apply
var h=new Date(a,b,c,d,e,f,g);
//the date constructor remaps years 0-99 to 1900-1999
return a<100&&a>=0&&isFinite(h.getFullYear())&&h.setFullYear(a),h}function ta(a){var b=new Date(Date.UTC.apply(null,arguments));
//the Date.UTC function remaps years 0-99 to 1900-1999
return a<100&&a>=0&&isFinite(b.getUTCFullYear())&&b.setUTCFullYear(a),b}
// start-of-first-week - start-of-year
function ua(a,b,c){var// first-week day -- which january is always in the first week (4 for iso, 1 for other)
d=7+b-c,
// first-week day local weekday -- which local weekday is fwd
e=(7+ta(a,0,d).getUTCDay()-b)%7;return-e+d-1}
//http://en.wikipedia.org/wiki/ISO_week_date#Calculating_a_date_given_the_year.2C_week_number_and_weekday
function va(a,b,c,d,e){var f,g,h=(7+c-d)%7,i=ua(a,d,e),j=1+7*(b-1)+h+i;return j<=0?(f=a-1,g=pa(f)+j):j>pa(a)?(f=a+1,g=j-pa(a)):(f=a,g=j),{year:f,dayOfYear:g}}function wa(a,b,c){var d,e,f=ua(a.year(),b,c),g=Math.floor((a.dayOfYear()-f-1)/7)+1;return g<1?(e=a.year()-1,d=g+xa(e,b,c)):g>xa(a.year(),b,c)?(d=g-xa(a.year(),b,c),e=a.year()+1):(e=a.year(),d=g),{week:d,year:e}}function xa(a,b,c){var d=ua(a,b,c),e=ua(a+1,b,c);return(pa(a)-d+e)/7}
// HELPERS
// LOCALES
function ya(a){return wa(a,this._week.dow,this._week.doy).week}function za(){return this._week.dow}function Aa(){return this._week.doy}
// MOMENTS
function Ba(a){var b=this.localeData().week(this);return null==a?b:this.add(7*(a-b),"d")}function Ca(a){var b=wa(this,1,4).week;return null==a?b:this.add(7*(a-b),"d")}
// HELPERS
function Da(a,b){return"string"!=typeof a?a:isNaN(a)?(a=b.weekdaysParse(a),"number"==typeof a?a:null):parseInt(a,10)}function Ea(a,b){return"string"==typeof a?b.weekdaysParse(a)%7||7:isNaN(a)?null:a}function Fa(a,b){return a?c(this._weekdays)?this._weekdays[a.day()]:this._weekdays[this._weekdays.isFormat.test(b)?"format":"standalone"][a.day()]:this._weekdays}function Ga(a){return a?this._weekdaysShort[a.day()]:this._weekdaysShort}function Ha(a){return a?this._weekdaysMin[a.day()]:this._weekdaysMin}function Ia(a,b,c){var d,e,f,g=a.toLocaleLowerCase();if(!this._weekdaysParse)for(this._weekdaysParse=[],this._shortWeekdaysParse=[],this._minWeekdaysParse=[],d=0;d<7;++d)f=k([2e3,1]).day(d),this._minWeekdaysParse[d]=this.weekdaysMin(f,"").toLocaleLowerCase(),this._shortWeekdaysParse[d]=this.weekdaysShort(f,"").toLocaleLowerCase(),this._weekdaysParse[d]=this.weekdays(f,"").toLocaleLowerCase();return c?"dddd"===b?(e=je.call(this._weekdaysParse,g),e!==-1?e:null):"ddd"===b?(e=je.call(this._shortWeekdaysParse,g),e!==-1?e:null):(e=je.call(this._minWeekdaysParse,g),e!==-1?e:null):"dddd"===b?(e=je.call(this._weekdaysParse,g),e!==-1?e:(e=je.call(this._shortWeekdaysParse,g),e!==-1?e:(e=je.call(this._minWeekdaysParse,g),e!==-1?e:null))):"ddd"===b?(e=je.call(this._shortWeekdaysParse,g),e!==-1?e:(e=je.call(this._weekdaysParse,g),e!==-1?e:(e=je.call(this._minWeekdaysParse,g),e!==-1?e:null))):(e=je.call(this._minWeekdaysParse,g),e!==-1?e:(e=je.call(this._weekdaysParse,g),e!==-1?e:(e=je.call(this._shortWeekdaysParse,g),e!==-1?e:null)))}function Ja(a,b,c){var d,e,f;if(this._weekdaysParseExact)return Ia.call(this,a,b,c);for(this._weekdaysParse||(this._weekdaysParse=[],this._minWeekdaysParse=[],this._shortWeekdaysParse=[],this._fullWeekdaysParse=[]),d=0;d<7;d++){
// test the regex
if(
// make the regex if we don't have it already
e=k([2e3,1]).day(d),c&&!this._fullWeekdaysParse[d]&&(this._fullWeekdaysParse[d]=new RegExp("^"+this.weekdays(e,"").replace(".",".?")+"$","i"),this._shortWeekdaysParse[d]=new RegExp("^"+this.weekdaysShort(e,"").replace(".",".?")+"$","i"),this._minWeekdaysParse[d]=new RegExp("^"+this.weekdaysMin(e,"").replace(".",".?")+"$","i")),this._weekdaysParse[d]||(f="^"+this.weekdays(e,"")+"|^"+this.weekdaysShort(e,"")+"|^"+this.weekdaysMin(e,""),this._weekdaysParse[d]=new RegExp(f.replace(".",""),"i")),c&&"dddd"===b&&this._fullWeekdaysParse[d].test(a))return d;if(c&&"ddd"===b&&this._shortWeekdaysParse[d].test(a))return d;if(c&&"dd"===b&&this._minWeekdaysParse[d].test(a))return d;if(!c&&this._weekdaysParse[d].test(a))return d}}
// MOMENTS
function Ka(a){if(!this.isValid())return null!=a?this:NaN;var b=this._isUTC?this._d.getUTCDay():this._d.getDay();return null!=a?(a=Da(a,this.localeData()),this.add(a-b,"d")):b}function La(a){if(!this.isValid())return null!=a?this:NaN;var b=(this.day()+7-this.localeData()._week.dow)%7;return null==a?b:this.add(a-b,"d")}function Ma(a){if(!this.isValid())return null!=a?this:NaN;
// behaves the same as moment#day except
// as a getter, returns 7 instead of 0 (1-7 range instead of 0-6)
// as a setter, sunday should belong to the previous week.
if(null!=a){var b=Ea(a,this.localeData());return this.day(this.day()%7?b:b-7)}return this.day()||7}function Na(a){return this._weekdaysParseExact?(i(this,"_weekdaysRegex")||Qa.call(this),a?this._weekdaysStrictRegex:this._weekdaysRegex):(i(this,"_weekdaysRegex")||(this._weekdaysRegex=ue),this._weekdaysStrictRegex&&a?this._weekdaysStrictRegex:this._weekdaysRegex)}function Oa(a){return this._weekdaysParseExact?(i(this,"_weekdaysRegex")||Qa.call(this),a?this._weekdaysShortStrictRegex:this._weekdaysShortRegex):(i(this,"_weekdaysShortRegex")||(this._weekdaysShortRegex=ve),this._weekdaysShortStrictRegex&&a?this._weekdaysShortStrictRegex:this._weekdaysShortRegex)}function Pa(a){return this._weekdaysParseExact?(i(this,"_weekdaysRegex")||Qa.call(this),a?this._weekdaysMinStrictRegex:this._weekdaysMinRegex):(i(this,"_weekdaysMinRegex")||(this._weekdaysMinRegex=we),this._weekdaysMinStrictRegex&&a?this._weekdaysMinStrictRegex:this._weekdaysMinRegex)}function Qa(){function a(a,b){return b.length-a.length}var b,c,d,e,f,g=[],h=[],i=[],j=[];for(b=0;b<7;b++)
// make the regex if we don't have it already
c=k([2e3,1]).day(b),d=this.weekdaysMin(c,""),e=this.weekdaysShort(c,""),f=this.weekdays(c,""),g.push(d),h.push(e),i.push(f),j.push(d),j.push(e),j.push(f);for(
// Sorting makes sure if one weekday (or abbr) is a prefix of another it
// will match the longer piece.
g.sort(a),h.sort(a),i.sort(a),j.sort(a),b=0;b<7;b++)h[b]=aa(h[b]),i[b]=aa(i[b]),j[b]=aa(j[b]);this._weekdaysRegex=new RegExp("^("+j.join("|")+")","i"),this._weekdaysShortRegex=this._weekdaysRegex,this._weekdaysMinRegex=this._weekdaysRegex,this._weekdaysStrictRegex=new RegExp("^("+i.join("|")+")","i"),this._weekdaysShortStrictRegex=new RegExp("^("+h.join("|")+")","i"),this._weekdaysMinStrictRegex=new RegExp("^("+g.join("|")+")","i")}
// FORMATTING
function Ra(){return this.hours()%12||12}function Sa(){return this.hours()||24}function Ta(a,b){U(a,0,0,function(){return this.localeData().meridiem(this.hours(),this.minutes(),b)})}
// PARSING
function Ua(a,b){return b._meridiemParse}
// LOCALES
function Va(a){
// IE8 Quirks Mode & IE7 Standards Mode do not allow accessing strings like arrays
// Using charAt should be more compatible.
return"p"===(a+"").toLowerCase().charAt(0)}function Wa(a,b,c){return a>11?c?"pm":"PM":c?"am":"AM"}function Xa(a){return a?a.toLowerCase().replace("_","-"):a}
// pick the locale from the array
// try ['en-au', 'en-gb'] as 'en-au', 'en-gb', 'en', as in move through the list trying each
// substring from most specific to least, but move to the next array item if it's a more specific variant than the current root
function Ya(a){for(var b,c,d,e,f=0;f<a.length;){for(e=Xa(a[f]).split("-"),b=e.length,c=Xa(a[f+1]),c=c?c.split("-"):null;b>0;){if(d=Za(e.slice(0,b).join("-")))return d;if(c&&c.length>=b&&v(e,c,!0)>=b-1)
//the next array item is better than a shallower substring of this one
break;b--}f++}return null}function Za(a){var b=null;
// TODO: Find a better way to register and load all the locales in Node
if(!Be[a]&&"undefined"!=typeof module&&module&&module.exports)try{b=xe._abbr,require("./locale/"+a),
// because defineLocale currently also sets the global locale, we
// want to undo that for lazy loaded locales
$a(b)}catch(a){}return Be[a]}
// This function will load locale and then set the global locale.  If
// no arguments are passed in, it will simply return the current global
// locale key.
function $a(a,b){var c;
// moment.duration._locale = moment._locale = data;
return a&&(c=p(b)?bb(a):_a(a,b),c&&(xe=c)),xe._abbr}function _a(a,b){if(null!==b){var c=Ae;if(b.abbr=a,null!=Be[a])y("defineLocaleOverride","use moment.updateLocale(localeName, config) to change an existing locale. moment.defineLocale(localeName, config) should only be used for creating a new locale See http://momentjs.com/guides/#/warnings/define-locale/ for more info."),c=Be[a]._config;else if(null!=b.parentLocale){if(null==Be[b.parentLocale])return Ce[b.parentLocale]||(Ce[b.parentLocale]=[]),Ce[b.parentLocale].push({name:a,config:b}),null;c=Be[b.parentLocale]._config}
// backwards compat for now: also set the locale
// make sure we set the locale AFTER all child locales have been
// created, so we won't end up with the child locale set.
return Be[a]=new C(B(c,b)),Ce[a]&&Ce[a].forEach(function(a){_a(a.name,a.config)}),$a(a),Be[a]}
// useful for testing
return delete Be[a],null}function ab(a,b){if(null!=b){var c,d=Ae;
// MERGE
null!=Be[a]&&(d=Be[a]._config),b=B(d,b),c=new C(b),c.parentLocale=Be[a],Be[a]=c,
// backwards compat for now: also set the locale
$a(a)}else
// pass null for config to unupdate, useful for tests
null!=Be[a]&&(null!=Be[a].parentLocale?Be[a]=Be[a].parentLocale:null!=Be[a]&&delete Be[a]);return Be[a]}
// returns locale data
function bb(a){var b;if(a&&a._locale&&a._locale._abbr&&(a=a._locale._abbr),!a)return xe;if(!c(a)){if(
//short-circuit everything else
b=Za(a))return b;a=[a]}return Ya(a)}function cb(){return wd(Be)}function db(a){var b,c=a._a;return c&&m(a).overflow===-2&&(b=c[be]<0||c[be]>11?be:c[ce]<1||c[ce]>ea(c[ae],c[be])?ce:c[de]<0||c[de]>24||24===c[de]&&(0!==c[ee]||0!==c[fe]||0!==c[ge])?de:c[ee]<0||c[ee]>59?ee:c[fe]<0||c[fe]>59?fe:c[ge]<0||c[ge]>999?ge:-1,m(a)._overflowDayOfYear&&(b<ae||b>ce)&&(b=ce),m(a)._overflowWeeks&&b===-1&&(b=he),m(a)._overflowWeekday&&b===-1&&(b=ie),m(a).overflow=b),a}
// date from iso format
function eb(a){var b,c,d,e,f,g,h=a._i,i=De.exec(h)||Ee.exec(h);if(i){for(m(a).iso=!0,b=0,c=Ge.length;b<c;b++)if(Ge[b][1].exec(i[1])){e=Ge[b][0],d=Ge[b][2]!==!1;break}if(null==e)return void(a._isValid=!1);if(i[3]){for(b=0,c=He.length;b<c;b++)if(He[b][1].exec(i[3])){
// match[2] should be 'T' or space
f=(i[2]||" ")+He[b][0];break}if(null==f)return void(a._isValid=!1)}if(!d&&null!=f)return void(a._isValid=!1);if(i[4]){if(!Fe.exec(i[4]))return void(a._isValid=!1);g="Z"}a._f=e+(f||"")+(g||""),kb(a)}else a._isValid=!1}
// date from iso format or fallback
function fb(b){var c=Ie.exec(b._i);return null!==c?void(b._d=new Date(+c[1])):(eb(b),void(b._isValid===!1&&(delete b._isValid,a.createFromInputFallback(b))))}
// Pick the first defined of two or three arguments.
function gb(a,b,c){return null!=a?a:null!=b?b:c}function hb(b){
// hooks is actually the exported moment object
var c=new Date(a.now());return b._useUTC?[c.getUTCFullYear(),c.getUTCMonth(),c.getUTCDate()]:[c.getFullYear(),c.getMonth(),c.getDate()]}
// convert an array to a date.
// the array should mirror the parameters below
// note: all values past the year are optional and will default to the lowest possible value.
// [year, month, day , hour, minute, second, millisecond]
function ib(a){var b,c,d,e,f=[];if(!a._d){
// Default to current date.
// * if no year, month, day of month are given, default to today
// * if day of month is given, default month and year
// * if month is given, default only year
// * if year is given, don't default anything
for(d=hb(a),
//compute day of the year from weeks and weekdays
a._w&&null==a._a[ce]&&null==a._a[be]&&jb(a),
//if the day of the year is set, figure out what it is
a._dayOfYear&&(e=gb(a._a[ae],d[ae]),a._dayOfYear>pa(e)&&(m(a)._overflowDayOfYear=!0),c=ta(e,0,a._dayOfYear),a._a[be]=c.getUTCMonth(),a._a[ce]=c.getUTCDate()),b=0;b<3&&null==a._a[b];++b)a._a[b]=f[b]=d[b];
// Zero out whatever was not defaulted, including time
for(;b<7;b++)a._a[b]=f[b]=null==a._a[b]?2===b?1:0:a._a[b];
// Check for 24:00:00.000
24===a._a[de]&&0===a._a[ee]&&0===a._a[fe]&&0===a._a[ge]&&(a._nextDay=!0,a._a[de]=0),a._d=(a._useUTC?ta:sa).apply(null,f),
// Apply timezone offset from input. The actual utcOffset can be changed
// with parseZone.
null!=a._tzm&&a._d.setUTCMinutes(a._d.getUTCMinutes()-a._tzm),a._nextDay&&(a._a[de]=24)}}function jb(a){var b,c,d,e,f,g,h,i;if(b=a._w,null!=b.GG||null!=b.W||null!=b.E)f=1,g=4,
// TODO: We need to take the current isoWeekYear, but that depends on
// how we interpret now (local, utc, fixed offset). So create
// a now version of current config (take local/utc/offset flags, and
// create now).
c=gb(b.GG,a._a[ae],wa(sb(),1,4).year),d=gb(b.W,1),e=gb(b.E,1),(e<1||e>7)&&(i=!0);else{f=a._locale._week.dow,g=a._locale._week.doy;var j=wa(sb(),f,g);c=gb(b.gg,a._a[ae],j.year),
// Default to current week.
d=gb(b.w,j.week),null!=b.d?(
// weekday -- low day numbers are considered next week
e=b.d,(e<0||e>6)&&(i=!0)):null!=b.e?(
// local weekday -- counting starts from begining of week
e=b.e+f,(b.e<0||b.e>6)&&(i=!0)):
// default to begining of week
e=f}d<1||d>xa(c,f,g)?m(a)._overflowWeeks=!0:null!=i?m(a)._overflowWeekday=!0:(h=va(c,d,e,f,g),a._a[ae]=h.year,a._dayOfYear=h.dayOfYear)}
// date from string and format string
function kb(b){
// TODO: Move this to another part of the creation flow to prevent circular deps
if(b._f===a.ISO_8601)return void eb(b);b._a=[],m(b).empty=!0;
// This array is used to make a Date, either with `new Date` or `Date.UTC`
var c,d,e,f,g,h=""+b._i,i=h.length,j=0;for(e=Y(b._f,b._locale).match(Fd)||[],c=0;c<e.length;c++)f=e[c],d=(h.match($(f,b))||[])[0],
// console.log('token', token, 'parsedInput', parsedInput,
//         'regex', getParseRegexForToken(token, config));
d&&(g=h.substr(0,h.indexOf(d)),g.length>0&&m(b).unusedInput.push(g),h=h.slice(h.indexOf(d)+d.length),j+=d.length),
// don't parse if it's not a known token
Id[f]?(d?m(b).empty=!1:m(b).unusedTokens.push(f),da(f,d,b)):b._strict&&!d&&m(b).unusedTokens.push(f);
// add remaining unparsed input length to the string
m(b).charsLeftOver=i-j,h.length>0&&m(b).unusedInput.push(h),
// clear _12h flag if hour is <= 12
b._a[de]<=12&&m(b).bigHour===!0&&b._a[de]>0&&(m(b).bigHour=void 0),m(b).parsedDateParts=b._a.slice(0),m(b).meridiem=b._meridiem,
// handle meridiem
b._a[de]=lb(b._locale,b._a[de],b._meridiem),ib(b),db(b)}function lb(a,b,c){var d;
// Fallback
return null==c?b:null!=a.meridiemHour?a.meridiemHour(b,c):null!=a.isPM?(d=a.isPM(c),d&&b<12&&(b+=12),d||12!==b||(b=0),b):b}
// date from string and array of format strings
function mb(a){var b,c,d,e,f;if(0===a._f.length)return m(a).invalidFormat=!0,void(a._d=new Date(NaN));for(e=0;e<a._f.length;e++)f=0,b=q({},a),null!=a._useUTC&&(b._useUTC=a._useUTC),b._f=a._f[e],kb(b),n(b)&&(
// if there is any input that was not parsed add a penalty for that format
f+=m(b).charsLeftOver,
//or tokens
f+=10*m(b).unusedTokens.length,m(b).score=f,(null==d||f<d)&&(d=f,c=b));j(a,c||b)}function nb(a){if(!a._d){var b=L(a._i);a._a=h([b.year,b.month,b.day||b.date,b.hour,b.minute,b.second,b.millisecond],function(a){return a&&parseInt(a,10)}),ib(a)}}function ob(a){var b=new r(db(pb(a)));
// Adding is smart enough around DST
return b._nextDay&&(b.add(1,"d"),b._nextDay=void 0),b}function pb(a){var b=a._i,d=a._f;return a._locale=a._locale||bb(a._l),null===b||void 0===d&&""===b?o({nullInput:!0}):("string"==typeof b&&(a._i=b=a._locale.preparse(b)),s(b)?new r(db(b)):(g(b)?a._d=b:c(d)?mb(a):d?kb(a):qb(a),n(a)||(a._d=null),a))}function qb(b){var d=b._i;void 0===d?b._d=new Date(a.now()):g(d)?b._d=new Date(d.valueOf()):"string"==typeof d?fb(b):c(d)?(b._a=h(d.slice(0),function(a){return parseInt(a,10)}),ib(b)):"object"==typeof d?nb(b):f(d)?
// from milliseconds
b._d=new Date(d):a.createFromInputFallback(b)}function rb(a,b,f,g,h){var i={};
// object construction must be done this way.
// https://github.com/moment/moment/issues/1423
return f!==!0&&f!==!1||(g=f,f=void 0),(d(a)&&e(a)||c(a)&&0===a.length)&&(a=void 0),i._isAMomentObject=!0,i._useUTC=i._isUTC=h,i._l=f,i._i=a,i._f=b,i._strict=g,ob(i)}function sb(a,b,c,d){return rb(a,b,c,d,!1)}
// Pick a moment m from moments so that m[fn](other) is true for all
// other. This relies on the function fn to be transitive.
//
// moments should either be an array of moment objects or an array, whose
// first element is an array of moment objects.
function tb(a,b){var d,e;if(1===b.length&&c(b[0])&&(b=b[0]),!b.length)return sb();for(d=b[0],e=1;e<b.length;++e)b[e].isValid()&&!b[e][a](d)||(d=b[e]);return d}
// TODO: Use [].sort instead?
function ub(){var a=[].slice.call(arguments,0);return tb("isBefore",a)}function vb(){var a=[].slice.call(arguments,0);return tb("isAfter",a)}function wb(a){var b=L(a),c=b.year||0,d=b.quarter||0,e=b.month||0,f=b.week||0,g=b.day||0,h=b.hour||0,i=b.minute||0,j=b.second||0,k=b.millisecond||0;
// representation for dateAddRemove
this._milliseconds=+k+1e3*j+// 1000
6e4*i+// 1000 * 60
1e3*h*60*60,//using 1000 * 60 * 60 instead of 36e5 to avoid floating point rounding errors https://github.com/moment/moment/issues/2978
// Because of dateAddRemove treats 24 hours as different from a
// day when working around DST, we need to store them separately
this._days=+g+7*f,
// It is impossible translate months into days without knowing
// which months you are are talking about, so we have to store
// it separately.
this._months=+e+3*d+12*c,this._data={},this._locale=bb(),this._bubble()}function xb(a){return a instanceof wb}function yb(a){return a<0?Math.round(-1*a)*-1:Math.round(a)}
// FORMATTING
function zb(a,b){U(a,0,0,function(){var a=this.utcOffset(),c="+";return a<0&&(a=-a,c="-"),c+T(~~(a/60),2)+b+T(~~a%60,2)})}function Ab(a,b){var c=(b||"").match(a);if(null===c)return null;var d=c[c.length-1]||[],e=(d+"").match(Me)||["-",0,0],f=+(60*e[1])+u(e[2]);return 0===f?0:"+"===e[0]?f:-f}
// Return a moment from input, that is local/utc/zone equivalent to model.
function Bb(b,c){var d,e;
// Use low-level api, because this fn is low-level api.
return c._isUTC?(d=c.clone(),e=(s(b)||g(b)?b.valueOf():sb(b).valueOf())-d.valueOf(),d._d.setTime(d._d.valueOf()+e),a.updateOffset(d,!1),d):sb(b).local()}function Cb(a){
// On Firefox.24 Date#getTimezoneOffset returns a floating point.
// https://github.com/moment/moment/pull/1871
return 15*-Math.round(a._d.getTimezoneOffset()/15)}
// MOMENTS
// keepLocalTime = true means only change the timezone, without
// affecting the local hour. So 5:31:26 +0300 --[utcOffset(2, true)]-->
// 5:31:26 +0200 It is possible that 5:31:26 doesn't exist with offset
// +0200, so we adjust the time as needed, to be valid.
//
// Keeping the time actually adds/subtracts (one hour)
// from the actual represented time. That is why we call updateOffset
// a second time. In case it wants us to change the offset again
// _changeInProgress == true case, then we have to adjust, because
// there is no such time in the given timezone.
function Db(b,c){var d,e=this._offset||0;if(!this.isValid())return null!=b?this:NaN;if(null!=b){if("string"==typeof b){if(b=Ab(Xd,b),null===b)return this}else Math.abs(b)<16&&(b=60*b);return!this._isUTC&&c&&(d=Cb(this)),this._offset=b,this._isUTC=!0,null!=d&&this.add(d,"m"),e!==b&&(!c||this._changeInProgress?Tb(this,Ob(b-e,"m"),1,!1):this._changeInProgress||(this._changeInProgress=!0,a.updateOffset(this,!0),this._changeInProgress=null)),this}return this._isUTC?e:Cb(this)}function Eb(a,b){return null!=a?("string"!=typeof a&&(a=-a),this.utcOffset(a,b),this):-this.utcOffset()}function Fb(a){return this.utcOffset(0,a)}function Gb(a){return this._isUTC&&(this.utcOffset(0,a),this._isUTC=!1,a&&this.subtract(Cb(this),"m")),this}function Hb(){if(null!=this._tzm)this.utcOffset(this._tzm);else if("string"==typeof this._i){var a=Ab(Wd,this._i);null!=a?this.utcOffset(a):this.utcOffset(0,!0)}return this}function Ib(a){return!!this.isValid()&&(a=a?sb(a).utcOffset():0,(this.utcOffset()-a)%60===0)}function Jb(){return this.utcOffset()>this.clone().month(0).utcOffset()||this.utcOffset()>this.clone().month(5).utcOffset()}function Kb(){if(!p(this._isDSTShifted))return this._isDSTShifted;var a={};if(q(a,this),a=pb(a),a._a){var b=a._isUTC?k(a._a):sb(a._a);this._isDSTShifted=this.isValid()&&v(a._a,b.toArray())>0}else this._isDSTShifted=!1;return this._isDSTShifted}function Lb(){return!!this.isValid()&&!this._isUTC}function Mb(){return!!this.isValid()&&this._isUTC}function Nb(){return!!this.isValid()&&(this._isUTC&&0===this._offset)}function Ob(a,b){var c,d,e,g=a,
// matching against regexp is expensive, do it on demand
h=null;// checks for null or undefined
return xb(a)?g={ms:a._milliseconds,d:a._days,M:a._months}:f(a)?(g={},b?g[b]=a:g.milliseconds=a):(h=Ne.exec(a))?(c="-"===h[1]?-1:1,g={y:0,d:u(h[ce])*c,h:u(h[de])*c,m:u(h[ee])*c,s:u(h[fe])*c,ms:u(yb(1e3*h[ge]))*c}):(h=Oe.exec(a))?(c="-"===h[1]?-1:1,g={y:Pb(h[2],c),M:Pb(h[3],c),w:Pb(h[4],c),d:Pb(h[5],c),h:Pb(h[6],c),m:Pb(h[7],c),s:Pb(h[8],c)}):null==g?g={}:"object"==typeof g&&("from"in g||"to"in g)&&(e=Rb(sb(g.from),sb(g.to)),g={},g.ms=e.milliseconds,g.M=e.months),d=new wb(g),xb(a)&&i(a,"_locale")&&(d._locale=a._locale),d}function Pb(a,b){
// We'd normally use ~~inp for this, but unfortunately it also
// converts floats to ints.
// inp may be undefined, so careful calling replace on it.
var c=a&&parseFloat(a.replace(",","."));
// apply sign while we're at it
return(isNaN(c)?0:c)*b}function Qb(a,b){var c={milliseconds:0,months:0};return c.months=b.month()-a.month()+12*(b.year()-a.year()),a.clone().add(c.months,"M").isAfter(b)&&--c.months,c.milliseconds=+b-+a.clone().add(c.months,"M"),c}function Rb(a,b){var c;return a.isValid()&&b.isValid()?(b=Bb(b,a),a.isBefore(b)?c=Qb(a,b):(c=Qb(b,a),c.milliseconds=-c.milliseconds,c.months=-c.months),c):{milliseconds:0,months:0}}
// TODO: remove 'name' arg after deprecation is removed
function Sb(a,b){return function(c,d){var e,f;
//invert the arguments, but complain about it
return null===d||isNaN(+d)||(y(b,"moment()."+b+"(period, number) is deprecated. Please use moment()."+b+"(number, period). See http://momentjs.com/guides/#/warnings/add-inverted-param/ for more info."),f=c,c=d,d=f),c="string"==typeof c?+c:c,e=Ob(c,d),Tb(this,e,a),this}}function Tb(b,c,d,e){var f=c._milliseconds,g=yb(c._days),h=yb(c._months);b.isValid()&&(e=null==e||e,f&&b._d.setTime(b._d.valueOf()+f*d),g&&Q(b,"Date",P(b,"Date")+g*d),h&&ja(b,P(b,"Month")+h*d),e&&a.updateOffset(b,g||h))}function Ub(a,b){var c=a.diff(b,"days",!0);return c<-6?"sameElse":c<-1?"lastWeek":c<0?"lastDay":c<1?"sameDay":c<2?"nextDay":c<7?"nextWeek":"sameElse"}function Vb(b,c){
// We want to compare the start of today, vs this.
// Getting start-of-today depends on whether we're local/utc/offset or not.
var d=b||sb(),e=Bb(d,this).startOf("day"),f=a.calendarFormat(this,e)||"sameElse",g=c&&(z(c[f])?c[f].call(this,d):c[f]);return this.format(g||this.localeData().calendar(f,this,sb(d)))}function Wb(){return new r(this)}function Xb(a,b){var c=s(a)?a:sb(a);return!(!this.isValid()||!c.isValid())&&(b=K(p(b)?"millisecond":b),"millisecond"===b?this.valueOf()>c.valueOf():c.valueOf()<this.clone().startOf(b).valueOf())}function Yb(a,b){var c=s(a)?a:sb(a);return!(!this.isValid()||!c.isValid())&&(b=K(p(b)?"millisecond":b),"millisecond"===b?this.valueOf()<c.valueOf():this.clone().endOf(b).valueOf()<c.valueOf())}function Zb(a,b,c,d){return d=d||"()",("("===d[0]?this.isAfter(a,c):!this.isBefore(a,c))&&(")"===d[1]?this.isBefore(b,c):!this.isAfter(b,c))}function $b(a,b){var c,d=s(a)?a:sb(a);return!(!this.isValid()||!d.isValid())&&(b=K(b||"millisecond"),"millisecond"===b?this.valueOf()===d.valueOf():(c=d.valueOf(),this.clone().startOf(b).valueOf()<=c&&c<=this.clone().endOf(b).valueOf()))}function _b(a,b){return this.isSame(a,b)||this.isAfter(a,b)}function ac(a,b){return this.isSame(a,b)||this.isBefore(a,b)}function bc(a,b,c){var d,e,f,g;// 1000
// 1000 * 60
// 1000 * 60 * 60
// 1000 * 60 * 60 * 24, negate dst
// 1000 * 60 * 60 * 24 * 7, negate dst
return this.isValid()?(d=Bb(a,this),d.isValid()?(e=6e4*(d.utcOffset()-this.utcOffset()),b=K(b),"year"===b||"month"===b||"quarter"===b?(g=cc(this,d),"quarter"===b?g/=3:"year"===b&&(g/=12)):(f=this-d,g="second"===b?f/1e3:"minute"===b?f/6e4:"hour"===b?f/36e5:"day"===b?(f-e)/864e5:"week"===b?(f-e)/6048e5:f),c?g:t(g)):NaN):NaN}function cc(a,b){
// difference in months
var c,d,e=12*(b.year()-a.year())+(b.month()-a.month()),
// b is in (anchor - 1 month, anchor + 1 month)
f=a.clone().add(e,"months");
//check for negative zero, return zero if negative zero
// linear across the month
// linear across the month
return b-f<0?(c=a.clone().add(e-1,"months"),d=(b-f)/(f-c)):(c=a.clone().add(e+1,"months"),d=(b-f)/(c-f)),-(e+d)||0}function dc(){return this.clone().locale("en").format("ddd MMM DD YYYY HH:mm:ss [GMT]ZZ")}function ec(){var a=this.clone().utc();return 0<a.year()&&a.year()<=9999?z(Date.prototype.toISOString)?this.toDate().toISOString():X(a,"YYYY-MM-DD[T]HH:mm:ss.SSS[Z]"):X(a,"YYYYYY-MM-DD[T]HH:mm:ss.SSS[Z]")}/**
 * Return a human readable representation of a moment that can
 * also be evaluated to get a new moment which is the same
 *
 * @link https://nodejs.org/dist/latest/docs/api/util.html#util_custom_inspect_function_on_objects
 */
function fc(){if(!this.isValid())return"moment.invalid(/* "+this._i+" */)";var a="moment",b="";this.isLocal()||(a=0===this.utcOffset()?"moment.utc":"moment.parseZone",b="Z");var c="["+a+'("]',d=0<this.year()&&this.year()<=9999?"YYYY":"YYYYYY",e="-MM-DD[T]HH:mm:ss.SSS",f=b+'[")]';return this.format(c+d+e+f)}function gc(b){b||(b=this.isUtc()?a.defaultFormatUtc:a.defaultFormat);var c=X(this,b);return this.localeData().postformat(c)}function hc(a,b){return this.isValid()&&(s(a)&&a.isValid()||sb(a).isValid())?Ob({to:this,from:a}).locale(this.locale()).humanize(!b):this.localeData().invalidDate()}function ic(a){return this.from(sb(),a)}function jc(a,b){return this.isValid()&&(s(a)&&a.isValid()||sb(a).isValid())?Ob({from:this,to:a}).locale(this.locale()).humanize(!b):this.localeData().invalidDate()}function kc(a){return this.to(sb(),a)}
// If passed a locale key, it will set the locale for this
// instance.  Otherwise, it will return the locale configuration
// variables for this instance.
function lc(a){var b;return void 0===a?this._locale._abbr:(b=bb(a),null!=b&&(this._locale=b),this)}function mc(){return this._locale}function nc(a){
// the following switch intentionally omits break keywords
// to utilize falling through the cases.
switch(a=K(a)){case"year":this.month(0);/* falls through */
case"quarter":case"month":this.date(1);/* falls through */
case"week":case"isoWeek":case"day":case"date":this.hours(0);/* falls through */
case"hour":this.minutes(0);/* falls through */
case"minute":this.seconds(0);/* falls through */
case"second":this.milliseconds(0)}
// weeks are a special case
// quarters are also special
return"week"===a&&this.weekday(0),"isoWeek"===a&&this.isoWeekday(1),"quarter"===a&&this.month(3*Math.floor(this.month()/3)),this}function oc(a){
// 'date' is an alias for 'day', so it should be considered as such.
return a=K(a),void 0===a||"millisecond"===a?this:("date"===a&&(a="day"),this.startOf(a).add(1,"isoWeek"===a?"week":a).subtract(1,"ms"))}function pc(){return this._d.valueOf()-6e4*(this._offset||0)}function qc(){return Math.floor(this.valueOf()/1e3)}function rc(){return new Date(this.valueOf())}function sc(){var a=this;return[a.year(),a.month(),a.date(),a.hour(),a.minute(),a.second(),a.millisecond()]}function tc(){var a=this;return{years:a.year(),months:a.month(),date:a.date(),hours:a.hours(),minutes:a.minutes(),seconds:a.seconds(),milliseconds:a.milliseconds()}}function uc(){
// new Date(NaN).toJSON() === null
return this.isValid()?this.toISOString():null}function vc(){return n(this)}function wc(){return j({},m(this))}function xc(){return m(this).overflow}function yc(){return{input:this._i,format:this._f,locale:this._locale,isUTC:this._isUTC,strict:this._strict}}function zc(a,b){U(0,[a,a.length],0,b)}
// MOMENTS
function Ac(a){return Ec.call(this,a,this.week(),this.weekday(),this.localeData()._week.dow,this.localeData()._week.doy)}function Bc(a){return Ec.call(this,a,this.isoWeek(),this.isoWeekday(),1,4)}function Cc(){return xa(this.year(),1,4)}function Dc(){var a=this.localeData()._week;return xa(this.year(),a.dow,a.doy)}function Ec(a,b,c,d,e){var f;return null==a?wa(this,d,e).year:(f=xa(a,d,e),b>f&&(b=f),Fc.call(this,a,b,c,d,e))}function Fc(a,b,c,d,e){var f=va(a,b,c,d,e),g=ta(f.year,0,f.dayOfYear);return this.year(g.getUTCFullYear()),this.month(g.getUTCMonth()),this.date(g.getUTCDate()),this}
// MOMENTS
function Gc(a){return null==a?Math.ceil((this.month()+1)/3):this.month(3*(a-1)+this.month()%3)}
// HELPERS
// MOMENTS
function Hc(a){var b=Math.round((this.clone().startOf("day")-this.clone().startOf("year"))/864e5)+1;return null==a?b:this.add(a-b,"d")}function Ic(a,b){b[ge]=u(1e3*("0."+a))}
// MOMENTS
function Jc(){return this._isUTC?"UTC":""}function Kc(){return this._isUTC?"Coordinated Universal Time":""}function Lc(a){return sb(1e3*a)}function Mc(){return sb.apply(null,arguments).parseZone()}function Nc(a){return a}function Oc(a,b,c,d){var e=bb(),f=k().set(d,b);return e[c](f,a)}function Pc(a,b,c){if(f(a)&&(b=a,a=void 0),a=a||"",null!=b)return Oc(a,b,c,"month");var d,e=[];for(d=0;d<12;d++)e[d]=Oc(a,d,c,"month");return e}
// ()
// (5)
// (fmt, 5)
// (fmt)
// (true)
// (true, 5)
// (true, fmt, 5)
// (true, fmt)
function Qc(a,b,c,d){"boolean"==typeof a?(f(b)&&(c=b,b=void 0),b=b||""):(b=a,c=b,a=!1,f(b)&&(c=b,b=void 0),b=b||"");var e=bb(),g=a?e._week.dow:0;if(null!=c)return Oc(b,(c+g)%7,d,"day");var h,i=[];for(h=0;h<7;h++)i[h]=Oc(b,(h+g)%7,d,"day");return i}function Rc(a,b){return Pc(a,b,"months")}function Sc(a,b){return Pc(a,b,"monthsShort")}function Tc(a,b,c){return Qc(a,b,c,"weekdays")}function Uc(a,b,c){return Qc(a,b,c,"weekdaysShort")}function Vc(a,b,c){return Qc(a,b,c,"weekdaysMin")}function Wc(){var a=this._data;return this._milliseconds=Ze(this._milliseconds),this._days=Ze(this._days),this._months=Ze(this._months),a.milliseconds=Ze(a.milliseconds),a.seconds=Ze(a.seconds),a.minutes=Ze(a.minutes),a.hours=Ze(a.hours),a.months=Ze(a.months),a.years=Ze(a.years),this}function Xc(a,b,c,d){var e=Ob(b,c);return a._milliseconds+=d*e._milliseconds,a._days+=d*e._days,a._months+=d*e._months,a._bubble()}
// supports only 2.0-style add(1, 's') or add(duration)
function Yc(a,b){return Xc(this,a,b,1)}
// supports only 2.0-style subtract(1, 's') or subtract(duration)
function Zc(a,b){return Xc(this,a,b,-1)}function $c(a){return a<0?Math.floor(a):Math.ceil(a)}function _c(){var a,b,c,d,e,f=this._milliseconds,g=this._days,h=this._months,i=this._data;
// if we have a mix of positive and negative values, bubble down first
// check: https://github.com/moment/moment/issues/2166
// The following code bubbles up values, see the tests for
// examples of what that means.
// convert days to months
// 12 months -> 1 year
return f>=0&&g>=0&&h>=0||f<=0&&g<=0&&h<=0||(f+=864e5*$c(bd(h)+g),g=0,h=0),i.milliseconds=f%1e3,a=t(f/1e3),i.seconds=a%60,b=t(a/60),i.minutes=b%60,c=t(b/60),i.hours=c%24,g+=t(c/24),e=t(ad(g)),h+=e,g-=$c(bd(e)),d=t(h/12),h%=12,i.days=g,i.months=h,i.years=d,this}function ad(a){
// 400 years have 146097 days (taking into account leap year rules)
// 400 years have 12 months === 4800
return 4800*a/146097}function bd(a){
// the reverse of daysToMonths
return 146097*a/4800}function cd(a){var b,c,d=this._milliseconds;if(a=K(a),"month"===a||"year"===a)return b=this._days+d/864e5,c=this._months+ad(b),"month"===a?c:c/12;switch(
// handle milliseconds separately because of floating point math errors (issue #1867)
b=this._days+Math.round(bd(this._months)),a){case"week":return b/7+d/6048e5;case"day":return b+d/864e5;case"hour":return 24*b+d/36e5;case"minute":return 1440*b+d/6e4;case"second":return 86400*b+d/1e3;
// Math.floor prevents floating point math errors here
case"millisecond":return Math.floor(864e5*b)+d;default:throw new Error("Unknown unit "+a)}}
// TODO: Use this.as('ms')?
function dd(){return this._milliseconds+864e5*this._days+this._months%12*2592e6+31536e6*u(this._months/12)}function ed(a){return function(){return this.as(a)}}function fd(a){return a=K(a),this[a+"s"]()}function gd(a){return function(){return this._data[a]}}function hd(){return t(this.days()/7)}
// helper function for moment.fn.from, moment.fn.fromNow, and moment.duration.fn.humanize
function id(a,b,c,d,e){return e.relativeTime(b||1,!!c,a,d)}function jd(a,b,c){var d=Ob(a).abs(),e=of(d.as("s")),f=of(d.as("m")),g=of(d.as("h")),h=of(d.as("d")),i=of(d.as("M")),j=of(d.as("y")),k=e<pf.s&&["s",e]||f<=1&&["m"]||f<pf.m&&["mm",f]||g<=1&&["h"]||g<pf.h&&["hh",g]||h<=1&&["d"]||h<pf.d&&["dd",h]||i<=1&&["M"]||i<pf.M&&["MM",i]||j<=1&&["y"]||["yy",j];return k[2]=b,k[3]=+a>0,k[4]=c,id.apply(null,k)}
// This function allows you to set the rounding function for relative time strings
function kd(a){return void 0===a?of:"function"==typeof a&&(of=a,!0)}
// This function allows you to set a threshold for relative time strings
function ld(a,b){return void 0!==pf[a]&&(void 0===b?pf[a]:(pf[a]=b,!0))}function md(a){var b=this.localeData(),c=jd(this,!a,b);return a&&(c=b.pastFuture(+this,c)),b.postformat(c)}function nd(){
// for ISO strings we do not use the normal bubbling rules:
//  * milliseconds bubble up until they become hours
//  * days do not bubble at all
//  * months bubble up until they become years
// This is because there is no context-free conversion between hours and days
// (think of clock changes)
// and also not between days and months (28-31 days per month)
var a,b,c,d=qf(this._milliseconds)/1e3,e=qf(this._days),f=qf(this._months);
// 3600 seconds -> 60 minutes -> 1 hour
a=t(d/60),b=t(a/60),d%=60,a%=60,
// 12 months -> 1 year
c=t(f/12),f%=12;
// inspired by https://github.com/dordille/moment-isoduration/blob/master/moment.isoduration.js
var g=c,h=f,i=e,j=b,k=a,l=d,m=this.asSeconds();return m?(m<0?"-":"")+"P"+(g?g+"Y":"")+(h?h+"M":"")+(i?i+"D":"")+(j||k||l?"T":"")+(j?j+"H":"")+(k?k+"M":"")+(l?l+"S":""):"P0D"}var od,pd;pd=Array.prototype.some?Array.prototype.some:function(a){for(var b=Object(this),c=b.length>>>0,d=0;d<c;d++)if(d in b&&a.call(this,b[d],d,b))return!0;return!1};var qd=pd,rd=a.momentProperties=[],sd=!1,td={};a.suppressDeprecationWarnings=!1,a.deprecationHandler=null;var ud;ud=Object.keys?Object.keys:function(a){var b,c=[];for(b in a)i(a,b)&&c.push(b);return c};var vd,wd=ud,xd={sameDay:"[Today at] LT",nextDay:"[Tomorrow at] LT",nextWeek:"dddd [at] LT",lastDay:"[Yesterday at] LT",lastWeek:"[Last] dddd [at] LT",sameElse:"L"},yd={LTS:"h:mm:ss A",LT:"h:mm A",L:"MM/DD/YYYY",LL:"MMMM D, YYYY",LLL:"MMMM D, YYYY h:mm A",LLLL:"dddd, MMMM D, YYYY h:mm A"},zd="Invalid date",Ad="%d",Bd=/\d{1,2}/,Cd={future:"in %s",past:"%s ago",s:"a few seconds",m:"a minute",mm:"%d minutes",h:"an hour",hh:"%d hours",d:"a day",dd:"%d days",M:"a month",MM:"%d months",y:"a year",yy:"%d years"},Dd={},Ed={},Fd=/(\[[^\[]*\])|(\\)?([Hh]mm(ss)?|Mo|MM?M?M?|Do|DDDo|DD?D?D?|ddd?d?|do?|w[o|w]?|W[o|W]?|Qo?|YYYYYY|YYYYY|YYYY|YY|gg(ggg?)?|GG(GGG?)?|e|E|a|A|hh?|HH?|kk?|mm?|ss?|S{1,9}|x|X|zz?|ZZ?|.)/g,Gd=/(\[[^\[]*\])|(\\)?(LTS|LT|LL?L?L?|l{1,4})/g,Hd={},Id={},Jd=/\d/,Kd=/\d\d/,Ld=/\d{3}/,Md=/\d{4}/,Nd=/[+-]?\d{6}/,Od=/\d\d?/,Pd=/\d\d\d\d?/,Qd=/\d\d\d\d\d\d?/,Rd=/\d{1,3}/,Sd=/\d{1,4}/,Td=/[+-]?\d{1,6}/,Ud=/\d+/,Vd=/[+-]?\d+/,Wd=/Z|[+-]\d\d:?\d\d/gi,Xd=/Z|[+-]\d\d(?::?\d\d)?/gi,Yd=/[+-]?\d+(\.\d{1,3})?/,Zd=/[0-9]*['a-z\u00A0-\u05FF\u0700-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]+|[\u0600-\u06FF\/]+(\s*?[\u0600-\u06FF]+){1,2}/i,$d={},_d={},ae=0,be=1,ce=2,de=3,ee=4,fe=5,ge=6,he=7,ie=8;vd=Array.prototype.indexOf?Array.prototype.indexOf:function(a){
// I know
var b;for(b=0;b<this.length;++b)if(this[b]===a)return b;return-1};var je=vd;
// FORMATTING
U("M",["MM",2],"Mo",function(){return this.month()+1}),U("MMM",0,0,function(a){return this.localeData().monthsShort(this,a)}),U("MMMM",0,0,function(a){return this.localeData().months(this,a)}),
// ALIASES
J("month","M"),
// PRIORITY
M("month",8),
// PARSING
Z("M",Od),Z("MM",Od,Kd),Z("MMM",function(a,b){return b.monthsShortRegex(a)}),Z("MMMM",function(a,b){return b.monthsRegex(a)}),ba(["M","MM"],function(a,b){b[be]=u(a)-1}),ba(["MMM","MMMM"],function(a,b,c,d){var e=c._locale.monthsParse(a,d,c._strict);
// if we didn't find a month name, mark the date as invalid.
null!=e?b[be]=e:m(c).invalidMonth=a});
// LOCALES
var ke=/D[oD]?(\[[^\[\]]*\]|\s)+MMMM?/,le="January_February_March_April_May_June_July_August_September_October_November_December".split("_"),me="Jan_Feb_Mar_Apr_May_Jun_Jul_Aug_Sep_Oct_Nov_Dec".split("_"),ne=Zd,oe=Zd;
// FORMATTING
U("Y",0,0,function(){var a=this.year();return a<=9999?""+a:"+"+a}),U(0,["YY",2],0,function(){return this.year()%100}),U(0,["YYYY",4],0,"year"),U(0,["YYYYY",5],0,"year"),U(0,["YYYYYY",6,!0],0,"year"),
// ALIASES
J("year","y"),
// PRIORITIES
M("year",1),
// PARSING
Z("Y",Vd),Z("YY",Od,Kd),Z("YYYY",Sd,Md),Z("YYYYY",Td,Nd),Z("YYYYYY",Td,Nd),ba(["YYYYY","YYYYYY"],ae),ba("YYYY",function(b,c){c[ae]=2===b.length?a.parseTwoDigitYear(b):u(b)}),ba("YY",function(b,c){c[ae]=a.parseTwoDigitYear(b)}),ba("Y",function(a,b){b[ae]=parseInt(a,10)}),
// HOOKS
a.parseTwoDigitYear=function(a){return u(a)+(u(a)>68?1900:2e3)};
// MOMENTS
var pe=O("FullYear",!0);
// FORMATTING
U("w",["ww",2],"wo","week"),U("W",["WW",2],"Wo","isoWeek"),
// ALIASES
J("week","w"),J("isoWeek","W"),
// PRIORITIES
M("week",5),M("isoWeek",5),
// PARSING
Z("w",Od),Z("ww",Od,Kd),Z("W",Od),Z("WW",Od,Kd),ca(["w","ww","W","WW"],function(a,b,c,d){b[d.substr(0,1)]=u(a)});var qe={dow:0,// Sunday is the first day of the week.
doy:6};
// FORMATTING
U("d",0,"do","day"),U("dd",0,0,function(a){return this.localeData().weekdaysMin(this,a)}),U("ddd",0,0,function(a){return this.localeData().weekdaysShort(this,a)}),U("dddd",0,0,function(a){return this.localeData().weekdays(this,a)}),U("e",0,0,"weekday"),U("E",0,0,"isoWeekday"),
// ALIASES
J("day","d"),J("weekday","e"),J("isoWeekday","E"),
// PRIORITY
M("day",11),M("weekday",11),M("isoWeekday",11),
// PARSING
Z("d",Od),Z("e",Od),Z("E",Od),Z("dd",function(a,b){return b.weekdaysMinRegex(a)}),Z("ddd",function(a,b){return b.weekdaysShortRegex(a)}),Z("dddd",function(a,b){return b.weekdaysRegex(a)}),ca(["dd","ddd","dddd"],function(a,b,c,d){var e=c._locale.weekdaysParse(a,d,c._strict);
// if we didn't get a weekday name, mark the date as invalid
null!=e?b.d=e:m(c).invalidWeekday=a}),ca(["d","e","E"],function(a,b,c,d){b[d]=u(a)});
// LOCALES
var re="Sunday_Monday_Tuesday_Wednesday_Thursday_Friday_Saturday".split("_"),se="Sun_Mon_Tue_Wed_Thu_Fri_Sat".split("_"),te="Su_Mo_Tu_We_Th_Fr_Sa".split("_"),ue=Zd,ve=Zd,we=Zd;U("H",["HH",2],0,"hour"),U("h",["hh",2],0,Ra),U("k",["kk",2],0,Sa),U("hmm",0,0,function(){return""+Ra.apply(this)+T(this.minutes(),2)}),U("hmmss",0,0,function(){return""+Ra.apply(this)+T(this.minutes(),2)+T(this.seconds(),2)}),U("Hmm",0,0,function(){return""+this.hours()+T(this.minutes(),2)}),U("Hmmss",0,0,function(){return""+this.hours()+T(this.minutes(),2)+T(this.seconds(),2)}),Ta("a",!0),Ta("A",!1),
// ALIASES
J("hour","h"),
// PRIORITY
M("hour",13),Z("a",Ua),Z("A",Ua),Z("H",Od),Z("h",Od),Z("HH",Od,Kd),Z("hh",Od,Kd),Z("hmm",Pd),Z("hmmss",Qd),Z("Hmm",Pd),Z("Hmmss",Qd),ba(["H","HH"],de),ba(["a","A"],function(a,b,c){c._isPm=c._locale.isPM(a),c._meridiem=a}),ba(["h","hh"],function(a,b,c){b[de]=u(a),m(c).bigHour=!0}),ba("hmm",function(a,b,c){var d=a.length-2;b[de]=u(a.substr(0,d)),b[ee]=u(a.substr(d)),m(c).bigHour=!0}),ba("hmmss",function(a,b,c){var d=a.length-4,e=a.length-2;b[de]=u(a.substr(0,d)),b[ee]=u(a.substr(d,2)),b[fe]=u(a.substr(e)),m(c).bigHour=!0}),ba("Hmm",function(a,b,c){var d=a.length-2;b[de]=u(a.substr(0,d)),b[ee]=u(a.substr(d))}),ba("Hmmss",function(a,b,c){var d=a.length-4,e=a.length-2;b[de]=u(a.substr(0,d)),b[ee]=u(a.substr(d,2)),b[fe]=u(a.substr(e))});var xe,ye=/[ap]\.?m?\.?/i,ze=O("Hours",!0),Ae={calendar:xd,longDateFormat:yd,invalidDate:zd,ordinal:Ad,ordinalParse:Bd,relativeTime:Cd,months:le,monthsShort:me,week:qe,weekdays:re,weekdaysMin:te,weekdaysShort:se,meridiemParse:ye},Be={},Ce={},De=/^\s*((?:[+-]\d{6}|\d{4})-(?:\d\d-\d\d|W\d\d-\d|W\d\d|\d\d\d|\d\d))(?:(T| )(\d\d(?::\d\d(?::\d\d(?:[.,]\d+)?)?)?)([\+\-]\d\d(?::?\d\d)?|\s*Z)?)?$/,Ee=/^\s*((?:[+-]\d{6}|\d{4})(?:\d\d\d\d|W\d\d\d|W\d\d|\d\d\d|\d\d))(?:(T| )(\d\d(?:\d\d(?:\d\d(?:[.,]\d+)?)?)?)([\+\-]\d\d(?::?\d\d)?|\s*Z)?)?$/,Fe=/Z|[+-]\d\d(?::?\d\d)?/,Ge=[["YYYYYY-MM-DD",/[+-]\d{6}-\d\d-\d\d/],["YYYY-MM-DD",/\d{4}-\d\d-\d\d/],["GGGG-[W]WW-E",/\d{4}-W\d\d-\d/],["GGGG-[W]WW",/\d{4}-W\d\d/,!1],["YYYY-DDD",/\d{4}-\d{3}/],["YYYY-MM",/\d{4}-\d\d/,!1],["YYYYYYMMDD",/[+-]\d{10}/],["YYYYMMDD",/\d{8}/],
// YYYYMM is NOT allowed by the standard
["GGGG[W]WWE",/\d{4}W\d{3}/],["GGGG[W]WW",/\d{4}W\d{2}/,!1],["YYYYDDD",/\d{7}/]],He=[["HH:mm:ss.SSSS",/\d\d:\d\d:\d\d\.\d+/],["HH:mm:ss,SSSS",/\d\d:\d\d:\d\d,\d+/],["HH:mm:ss",/\d\d:\d\d:\d\d/],["HH:mm",/\d\d:\d\d/],["HHmmss.SSSS",/\d\d\d\d\d\d\.\d+/],["HHmmss,SSSS",/\d\d\d\d\d\d,\d+/],["HHmmss",/\d\d\d\d\d\d/],["HHmm",/\d\d\d\d/],["HH",/\d\d/]],Ie=/^\/?Date\((\-?\d+)/i;a.createFromInputFallback=x("value provided is not in a recognized ISO format. moment construction falls back to js Date(), which is not reliable across all browsers and versions. Non ISO date formats are discouraged and will be removed in an upcoming major release. Please refer to http://momentjs.com/guides/#/warnings/js-date/ for more info.",function(a){a._d=new Date(a._i+(a._useUTC?" UTC":""))}),
// constant that refers to the ISO standard
a.ISO_8601=function(){};var Je=x("moment().min is deprecated, use moment.max instead. http://momentjs.com/guides/#/warnings/min-max/",function(){var a=sb.apply(null,arguments);return this.isValid()&&a.isValid()?a<this?this:a:o()}),Ke=x("moment().max is deprecated, use moment.min instead. http://momentjs.com/guides/#/warnings/min-max/",function(){var a=sb.apply(null,arguments);return this.isValid()&&a.isValid()?a>this?this:a:o()}),Le=function(){return Date.now?Date.now():+new Date};zb("Z",":"),zb("ZZ",""),
// PARSING
Z("Z",Xd),Z("ZZ",Xd),ba(["Z","ZZ"],function(a,b,c){c._useUTC=!0,c._tzm=Ab(Xd,a)});
// HELPERS
// timezone chunker
// '+10:00' > ['10',  '00']
// '-1530'  > ['-15', '30']
var Me=/([\+\-]|\d\d)/gi;
// HOOKS
// This function will be called whenever a moment is mutated.
// It is intended to keep the offset in sync with the timezone.
a.updateOffset=function(){};
// ASP.NET json date format regex
var Ne=/^(\-)?(?:(\d*)[. ])?(\d+)\:(\d+)(?:\:(\d+)(\.\d*)?)?$/,Oe=/^(-)?P(?:(-?[0-9,.]*)Y)?(?:(-?[0-9,.]*)M)?(?:(-?[0-9,.]*)W)?(?:(-?[0-9,.]*)D)?(?:T(?:(-?[0-9,.]*)H)?(?:(-?[0-9,.]*)M)?(?:(-?[0-9,.]*)S)?)?$/;Ob.fn=wb.prototype;var Pe=Sb(1,"add"),Qe=Sb(-1,"subtract");a.defaultFormat="YYYY-MM-DDTHH:mm:ssZ",a.defaultFormatUtc="YYYY-MM-DDTHH:mm:ss[Z]";var Re=x("moment().lang() is deprecated. Instead, use moment().localeData() to get the language configuration. Use moment().locale() to change languages.",function(a){return void 0===a?this.localeData():this.locale(a)});
// FORMATTING
U(0,["gg",2],0,function(){return this.weekYear()%100}),U(0,["GG",2],0,function(){return this.isoWeekYear()%100}),zc("gggg","weekYear"),zc("ggggg","weekYear"),zc("GGGG","isoWeekYear"),zc("GGGGG","isoWeekYear"),
// ALIASES
J("weekYear","gg"),J("isoWeekYear","GG"),
// PRIORITY
M("weekYear",1),M("isoWeekYear",1),
// PARSING
Z("G",Vd),Z("g",Vd),Z("GG",Od,Kd),Z("gg",Od,Kd),Z("GGGG",Sd,Md),Z("gggg",Sd,Md),Z("GGGGG",Td,Nd),Z("ggggg",Td,Nd),ca(["gggg","ggggg","GGGG","GGGGG"],function(a,b,c,d){b[d.substr(0,2)]=u(a)}),ca(["gg","GG"],function(b,c,d,e){c[e]=a.parseTwoDigitYear(b)}),
// FORMATTING
U("Q",0,"Qo","quarter"),
// ALIASES
J("quarter","Q"),
// PRIORITY
M("quarter",7),
// PARSING
Z("Q",Jd),ba("Q",function(a,b){b[be]=3*(u(a)-1)}),
// FORMATTING
U("D",["DD",2],"Do","date"),
// ALIASES
J("date","D"),
// PRIOROITY
M("date",9),
// PARSING
Z("D",Od),Z("DD",Od,Kd),Z("Do",function(a,b){return a?b._ordinalParse:b._ordinalParseLenient}),ba(["D","DD"],ce),ba("Do",function(a,b){b[ce]=u(a.match(Od)[0],10)});
// MOMENTS
var Se=O("Date",!0);
// FORMATTING
U("DDD",["DDDD",3],"DDDo","dayOfYear"),
// ALIASES
J("dayOfYear","DDD"),
// PRIORITY
M("dayOfYear",4),
// PARSING
Z("DDD",Rd),Z("DDDD",Ld),ba(["DDD","DDDD"],function(a,b,c){c._dayOfYear=u(a)}),
// FORMATTING
U("m",["mm",2],0,"minute"),
// ALIASES
J("minute","m"),
// PRIORITY
M("minute",14),
// PARSING
Z("m",Od),Z("mm",Od,Kd),ba(["m","mm"],ee);
// MOMENTS
var Te=O("Minutes",!1);
// FORMATTING
U("s",["ss",2],0,"second"),
// ALIASES
J("second","s"),
// PRIORITY
M("second",15),
// PARSING
Z("s",Od),Z("ss",Od,Kd),ba(["s","ss"],fe);
// MOMENTS
var Ue=O("Seconds",!1);
// FORMATTING
U("S",0,0,function(){return~~(this.millisecond()/100)}),U(0,["SS",2],0,function(){return~~(this.millisecond()/10)}),U(0,["SSS",3],0,"millisecond"),U(0,["SSSS",4],0,function(){return 10*this.millisecond()}),U(0,["SSSSS",5],0,function(){return 100*this.millisecond()}),U(0,["SSSSSS",6],0,function(){return 1e3*this.millisecond()}),U(0,["SSSSSSS",7],0,function(){return 1e4*this.millisecond()}),U(0,["SSSSSSSS",8],0,function(){return 1e5*this.millisecond()}),U(0,["SSSSSSSSS",9],0,function(){return 1e6*this.millisecond()}),
// ALIASES
J("millisecond","ms"),
// PRIORITY
M("millisecond",16),
// PARSING
Z("S",Rd,Jd),Z("SS",Rd,Kd),Z("SSS",Rd,Ld);var Ve;for(Ve="SSSS";Ve.length<=9;Ve+="S")Z(Ve,Ud);for(Ve="S";Ve.length<=9;Ve+="S")ba(Ve,Ic);
// MOMENTS
var We=O("Milliseconds",!1);
// FORMATTING
U("z",0,0,"zoneAbbr"),U("zz",0,0,"zoneName");var Xe=r.prototype;Xe.add=Pe,Xe.calendar=Vb,Xe.clone=Wb,Xe.diff=bc,Xe.endOf=oc,Xe.format=gc,Xe.from=hc,Xe.fromNow=ic,Xe.to=jc,Xe.toNow=kc,Xe.get=R,Xe.invalidAt=xc,Xe.isAfter=Xb,Xe.isBefore=Yb,Xe.isBetween=Zb,Xe.isSame=$b,Xe.isSameOrAfter=_b,Xe.isSameOrBefore=ac,Xe.isValid=vc,Xe.lang=Re,Xe.locale=lc,Xe.localeData=mc,Xe.max=Ke,Xe.min=Je,Xe.parsingFlags=wc,Xe.set=S,Xe.startOf=nc,Xe.subtract=Qe,Xe.toArray=sc,Xe.toObject=tc,Xe.toDate=rc,Xe.toISOString=ec,Xe.inspect=fc,Xe.toJSON=uc,Xe.toString=dc,Xe.unix=qc,Xe.valueOf=pc,Xe.creationData=yc,
// Year
Xe.year=pe,Xe.isLeapYear=ra,
// Week Year
Xe.weekYear=Ac,Xe.isoWeekYear=Bc,
// Quarter
Xe.quarter=Xe.quarters=Gc,
// Month
Xe.month=ka,Xe.daysInMonth=la,
// Week
Xe.week=Xe.weeks=Ba,Xe.isoWeek=Xe.isoWeeks=Ca,Xe.weeksInYear=Dc,Xe.isoWeeksInYear=Cc,
// Day
Xe.date=Se,Xe.day=Xe.days=Ka,Xe.weekday=La,Xe.isoWeekday=Ma,Xe.dayOfYear=Hc,
// Hour
Xe.hour=Xe.hours=ze,
// Minute
Xe.minute=Xe.minutes=Te,
// Second
Xe.second=Xe.seconds=Ue,
// Millisecond
Xe.millisecond=Xe.milliseconds=We,
// Offset
Xe.utcOffset=Db,Xe.utc=Fb,Xe.local=Gb,Xe.parseZone=Hb,Xe.hasAlignedHourOffset=Ib,Xe.isDST=Jb,Xe.isLocal=Lb,Xe.isUtcOffset=Mb,Xe.isUtc=Nb,Xe.isUTC=Nb,
// Timezone
Xe.zoneAbbr=Jc,Xe.zoneName=Kc,
// Deprecations
Xe.dates=x("dates accessor is deprecated. Use date instead.",Se),Xe.months=x("months accessor is deprecated. Use month instead",ka),Xe.years=x("years accessor is deprecated. Use year instead",pe),Xe.zone=x("moment().zone is deprecated, use moment().utcOffset instead. http://momentjs.com/guides/#/warnings/zone/",Eb),Xe.isDSTShifted=x("isDSTShifted is deprecated. See http://momentjs.com/guides/#/warnings/dst-shifted/ for more information",Kb);var Ye=C.prototype;Ye.calendar=D,Ye.longDateFormat=E,Ye.invalidDate=F,Ye.ordinal=G,Ye.preparse=Nc,Ye.postformat=Nc,Ye.relativeTime=H,Ye.pastFuture=I,Ye.set=A,
// Month
Ye.months=fa,Ye.monthsShort=ga,Ye.monthsParse=ia,Ye.monthsRegex=na,Ye.monthsShortRegex=ma,
// Week
Ye.week=ya,Ye.firstDayOfYear=Aa,Ye.firstDayOfWeek=za,
// Day of Week
Ye.weekdays=Fa,Ye.weekdaysMin=Ha,Ye.weekdaysShort=Ga,Ye.weekdaysParse=Ja,Ye.weekdaysRegex=Na,Ye.weekdaysShortRegex=Oa,Ye.weekdaysMinRegex=Pa,
// Hours
Ye.isPM=Va,Ye.meridiem=Wa,$a("en",{ordinalParse:/\d{1,2}(th|st|nd|rd)/,ordinal:function(a){var b=a%10,c=1===u(a%100/10)?"th":1===b?"st":2===b?"nd":3===b?"rd":"th";return a+c}}),
// Side effect imports
a.lang=x("moment.lang is deprecated. Use moment.locale instead.",$a),a.langData=x("moment.langData is deprecated. Use moment.localeData instead.",bb);var Ze=Math.abs,$e=ed("ms"),_e=ed("s"),af=ed("m"),bf=ed("h"),cf=ed("d"),df=ed("w"),ef=ed("M"),ff=ed("y"),gf=gd("milliseconds"),hf=gd("seconds"),jf=gd("minutes"),kf=gd("hours"),lf=gd("days"),mf=gd("months"),nf=gd("years"),of=Math.round,pf={s:45,// seconds to minute
m:45,// minutes to hour
h:22,// hours to day
d:26,// days to month
M:11},qf=Math.abs,rf=wb.prototype;
// Deprecations
// Side effect imports
// FORMATTING
// PARSING
// Side effect imports
return rf.abs=Wc,rf.add=Yc,rf.subtract=Zc,rf.as=cd,rf.asMilliseconds=$e,rf.asSeconds=_e,rf.asMinutes=af,rf.asHours=bf,rf.asDays=cf,rf.asWeeks=df,rf.asMonths=ef,rf.asYears=ff,rf.valueOf=dd,rf._bubble=_c,rf.get=fd,rf.milliseconds=gf,rf.seconds=hf,rf.minutes=jf,rf.hours=kf,rf.days=lf,rf.weeks=hd,rf.months=mf,rf.years=nf,rf.humanize=md,rf.toISOString=nd,rf.toString=nd,rf.toJSON=nd,rf.locale=lc,rf.localeData=mc,rf.toIsoString=x("toIsoString() is deprecated. Please use toISOString() instead (notice the capitals)",nd),rf.lang=Re,U("X",0,0,"unix"),U("x",0,0,"valueOf"),Z("x",Vd),Z("X",Yd),ba("X",function(a,b,c){c._d=new Date(1e3*parseFloat(a,10))}),ba("x",function(a,b,c){c._d=new Date(u(a))}),a.version="2.17.1",b(sb),a.fn=Xe,a.min=ub,a.max=vb,a.now=Le,a.utc=k,a.unix=Lc,a.months=Rc,a.isDate=g,a.locale=$a,a.invalid=o,a.duration=Ob,a.isMoment=s,a.weekdays=Tc,a.parseZone=Mc,a.localeData=bb,a.isDuration=xb,a.monthsShort=Sc,a.weekdaysMin=Vc,a.defineLocale=_a,a.updateLocale=ab,a.locales=cb,a.weekdaysShort=Uc,a.normalizeUnits=K,a.relativeTimeRounding=kd,a.relativeTimeThreshold=ld,a.calendarFormat=Ub,a.prototype=Xe,a});

(function (root, moduleFactory) {
    if (typeof exports === "object") {
        module.exports = moduleFactory(require("lodash"), require("moment"));
    } else if (typeof define === "function" && define.amd) {
        define(["lodash", "moment"], moduleFactory);
    } else if (typeof window !== 'undefined' && window != null && window.moment) {
        window.HandlebarsMoment = moduleFactory;
        // moduleFactory(_, moment);
    } else {
        root.HandlebarsMoment = moduleFactory(_, moment);
    }
}(this, function (_, moment) {
/**
 * @module handlebars%moment
 * @description  Helpers providing [Moment.js](http://momentjs.com) functionality
 *
 *     var Handlebars = require("handlebars");
 *     var MomentHelper = require("handlebars.moment");
 *     MomentHelper.registerHelpers(Handlebars);
 *
 * @returns {object} MomentHelper instance
 */

    var MomentHelpers = function (Handlebars) {
        var momentFormatMap = {
            dates: "date",
            months: "month",
            years: "year",
            isoweekday: "isoWeekday",
            dayofyear: "dayOfYear",
            isoweek: "isoWeek",
            isoweeks: "isoWeek",
            weekyear: "weekYear",
            isoweekyear: "isoWeekYear",
            zoneabbr: "zoneAbbr",
            zonename: "zoneName",
            tostring: "toString",
            string: "toString",
            str: "toString",
            valueof: "valueOf",
            value: "valueOf",
            val: "valueOf",
            fromnow: "fromNow",
            daysinmonth: "daysInMonth",
            todate: "toDate",
            toarray: "toArray",
            array: "toArray",
            tojson: "toJSON",
            json: "toJSON",
            toisostring: "toISOString",
            isostring: "toISOString"
        };
        var weekdayMap = {
            L: "dddd",
            S: "ddd",
            XS: "dd"
        };

        /**
         * @template moment
         * @block helper
         * @param {string|date} [0] Date string|object
         * @param {string} [1] Date format / Moment method
         * @param {*} [2] Moment method params
         * @param {*} [3] Moment method params 1
         * @param {*} [4] Moment method params 2
         * @param {string|date} [date] Alternative date
         * @param {string} [format] Alternative format
         * @param {string} [input] Format to use to parse date
         * @param {string} [lang] Specific locale to use
         * @param {number|string} [type] Type of weekday (L|S|XS|number)
         * @param {boolean} [suffix]
         * @param {boolean} [utc]
         * @param {boolean} [local]
         * @param {string} [from]
         * @param {string} [unixfrom]
         * @param {string} [max]
         * @param {string} [min]
         * @param {string} [unixmax]
         * @param {string} [unixmin]
         * @param {string} [startOf]
         * @param {string} [endOf]
         * @param {number|string} [add]
         * @param {number|string} [subtract]
         * @param {number} [addparam] Value to use if add is number
         * @param {number} [subtractparam] Value to use if subtract is number
         * @param {number} [amount] Value to use if add|subtract are strings
         * @param {string} [diff]
         * @param {string} [unixdiff]
         * @param {string} [unitdiff] Value for Moment method params 1 if diff
         * @param {boolean} [nosuffix] Value for Moment method params 2 if diff
         *
         * @description  Outputs a date
         *
         * Current date
         *
         *     {{moment}}
         *
         * Specific date
         *
         *     {{moment d}}
         *     {{moment date=d}}
         *
         * Date format
         *
         *     {{moment d "YY, MMM dd"}}
         *     {{moment date=d format="DD/MM/YYYY"}}
         *     {{moment d unix=true}}
         *
         * Date input parsing
         *
         *     {{moment dinput input="DD-YYYY-MM"}}
         *
         * Date timezone
         *
         *     {{moment dstr utc=true}}
         *     {{moment dstr local=true}}
         *
         * Date units
         *
         *     {{moment d "millisecond"}}
         *     {{moment d "second"}}
         *     {{moment d "minute"}}
         *     {{moment d "hour"}}
         *     {{moment d "date"}}
         *     {{moment d "day"}}
         *     {{moment d "weekday"}}
         *     {{moment d "weekday" type="s"}}
         *     {{moment d "weekday" type="xs"}}
         *     {{moment d "weekday" type="number"}}
         *     {{moment d "isoweekday"}}
         *     {{moment d "dayofyear"}}
         *     {{moment d "week"}}
         *     {{moment d "isoweek"}}
         *     {{moment d "month"}}
         *     {{moment d "year"}}
         *     {{moment d "weekyear"}}
         *     {{moment d "isoweekyear"}}
         *
         * Date manipulation
         *
         *     {{moment d add="days" amount="7"}}
         *     {{moment d add="365" addparam="d"}}
         *     {{moment d subtract="days" amount="7"}}
         *     {{moment d subtract="365" subtractparam="d"}}
         *
         * Start and end of years
         *
         *     {{moment d startof="year"}}
         *     {{moment d endof="year"}}
         *
         * Date max/min
         *
         *     {{moment d max=dmax}}
         *     {{moment d min=dmin}}
         *
         * Date from now and specific dates
         *
         *     {{moment d "fromNow"}}
         *     {{moment d "from" dfrom}}
         *
         * Difference between dates
         *
         *     {{moment d diff=ddiff}}
         *
         * Date as calendar time
         *
         *     {{moment d "calendar"}}
         *
         * Date as strings
         *
         *     {{moment d "str"}}
         *     {{moment d "val"}}
         *     {{moment d "unix"}}
         *
         * Moment utils
         *
         *     {{moment d "daysinmonth"}}
         *     {{moment d "todate"}}
         *     {{moment d "array"}}
         *     {{moment d "isostring"}}
         *
         * Helper parameters map to those of Moment.js unless noted
         */
        Handlebars.registerHelper("moment", function() {
            var args = Array.prototype.slice.call(arguments),
                options = args.pop(),
                date = args.shift(),
                format = args.shift(),
                formatParams = args.shift(),
                formatParams1 = args.shift(),
                formatParams2 = args.shift();

            if (options.hash && options.hash.params) {
                options.hash = _.extend({}, options.hash.params, options.hash);
                delete options.hash.params;
            }
            var params = options.hash;
            if (!date) {
                date = params.date;
            }
            function marshallDate (date, unix) {
                if (typeof date === "string" && date.match(/^\d+(\.\d+){0,1}$/)) {
                    date = +date;
                }
                if (unix && typeof date === "number") {
                    date = date * 1000;
                }
                return date;
            }
            date = marshallDate(date, params.unix);
            var max = marshallDate(params.max, params.unixmax);
            var min = marshallDate(params.min, params.unixmin);

            if (!format) {
                format = params.format || params.fn;
            }
            if (momentFormatMap[format]) {
                format = momentFormatMap[format];
            }
            if (format === "weekday") {
                params.type = params.type ? params.type.toUpperCase() : null;
                if (params.type !== "NUMBER") {
                    if (weekdayMap[params.type]) {
                        format = weekdayMap[params.type];
                    } else {
                        format = weekdayMap.L;
                    }
                }
            }

            var ofMethod = "start";
            var ofType = params.startOf || params.startof;
            if (!ofType) {
                ofType = params.endOf || params.endof;
                if (ofType) {
                    ofMethod = "end";
                }
            }

            /*if (!formatParams) {
                formatParams = params.formatparams || params.fnparams;
                if (formatParams === undefined) {
                    if (params.suffix !== undefined) {
                        formatParams = params.suffix;
                    }
                }
            }*/

            var momentObj;

            if (moment.isMoment(date)) {
                momentObj = date.clone();
            } else {
                var momentFn = params.utc ? moment.utc : moment;
                momentObj = momentFn(date, params.input);
            }

            if (params.lang) {
                momentObj.lang(params.lang);
            }

            if (max) {
                momentObj = moment.max(moment(max), momentObj);
            }
            if (min) {
                momentObj = moment.min(moment(min), momentObj);
            }

            if (ofType) {
                momentObj = momentObj[ofMethod + "Of"](ofType);
            }

            if (params.nosuffix === undefined && params.suffix !== undefined) {
                params.nosuffix = !params.suffix;
            }

            if (params.from) {
                format = "from";
                formatParams = marshallDate(params.from, params.unixfrom);
            }
            if (format === "fromNow") {
                if (formatParams === undefined) {
                    formatParams = params.nosuffix;
                }
            }
            if (format === "from") {
                if (formatParams1 === undefined) {
                    formatParams1 = params.nosuffix;
                }
            }
            if (params.diff) {
                format = "diff";
                formatParams = marshallDate(params.diff, params.unixdiff);
            }
            if (format === "diff") {
                if (formatParams1 === undefined) {
                    formatParams1 = params.unitdiff;
                }
                if (formatParams2 === undefined) {
                    formatParams2 = params.nosuffix;
                }
            }

            function manipulateMoment (method) {
                var arg = params[method];
                if (arg) {
                    var argParam = params[method + "param"];
                    if (argParam === undefined) {
                        argParam = params.amount;
                    }
                    var args = arg;
                    if (argParam) {
                        var addNum = +arg;
                        args = {};
                        if (isNaN(addNum)) {
                            args[arg] = +argParam;
                        } else {
                            args[argParam] = addNum;
                        }
                    }
                    momentObj[method](args);
                }
            }
            manipulateMoment("add");
            manipulateMoment("subtract");


            if (params.local) {
                momentObj.local();
            } else if (params.utc) {
                momentObj.utc();
            }

            var momentOutput = momentObj[format] ? momentObj[format](formatParams, formatParams1, formatParams2) : momentObj.format(format);

            return momentOutput;
        });

        var durationMethodMap = {
            asmilliseconds: "asMilliseconds",
            asseconds: "asSeconds",
            asminutes: "asMinutes",
            ashours: "asHours",
            asdays: "asDays",
            asweeks: "asWeeks",
            asmonths: "asMonths",
            asyears: "asYears"
        };
        var durationGetArray = [
            "ms",
            "s",
            "m",
            "h",
            "d",
            "w",
            "M",
            "y"
        ];
        var durationGetMap = {};
        for (var i in durationGetArray) {
            durationGetMap[durationGetArray[i]] = true;
        }

        /**
         * @template duration
         * @block helper
         * @param {string} [duration] Length of duration
         * @param {string} [input] Unit of duration
         * @param {number} [add]
         * @param {number} [subtract]
         * @param {number} [addunit|addparam]
         * @param {number} [subtractunit|subtractparam]
         * @param {string} [as] Sets duration method to as
         * @param {string} [get] Sets duration method to get
         * @param {string} [humanize] Sets duration method to humanize
         * @param {boolean} [suffix=false]
         *
         * @description  Outputs a duration
         *
         * Duration - implictly in milliseconds
         *
         *     {{duration d}}
         *
         * Duration with explicit input date unit type
         *
         *     {{duration d input="s"}}
         *
         * Amount of date units in duration
         *
         *     {{duration d "seconds"}}
         *     {{duration d "minutes"}}
         *     {{duration d "years"}}
         *     {{duration d get="seconds"}}
         *     {{duration d "s"}}
         *
         * Duration as date units
         *
         *     {{duration d "asseconds"}}
         *     {{duration d "asminutes"}}
         *     {{duration d "asyears"}}
         *     {{duration d as="seconds"}}
         *
         * Humanize duration output
         *
         *     {{duration d "humanize"}}
         *     {{duration d "humanize" true}}
         *     {{duration d suffix=true}}
         *
         * Manipulate durations
         *
         *     {{duration d add=damount as="milliseconds"}}
         *     {{duration d subtract=damount as="seconds"}}
         *     {{duration d subtract=damount as="days"}}
         */
        Handlebars.registerHelper("duration", function() {
            var args = Array.prototype.slice.call(arguments),
                options = args.pop(),
                duration = args.shift(),
                method = args.shift(),
                methodArg = args.shift();

            if (options.hash && options.hash.params) {
                options.hash = _.extend({}, options.hash.params, options.hash);
                delete options.hash.params;
            }
            var params = options.hash;
            if (!duration) {
                duration = params.duration;
            }
            if (typeof duration === "string" && duration.match(/^\d+$/)) {
                duration = +duration;
            }

            var durationObj = moment.duration(duration, params.input);

            function manipulateDuration (method) {
                var arg = params[method];
                if (arg) {
                    if (!isNaN(+arg)) {
                        arg = +arg;
                    }
                    var argParam = params[method + "unit"];
                    if (argParam === undefined) {
                        argParam = params[method + "param"];
                    }
                    durationObj[method](arg, argParam);
                }
            }
            manipulateDuration("add");
            manipulateDuration("subtract");

            if (!method) {
                method = params.method;
            }
            if (durationGetMap[method]) {
                methodArg = method;
                method = "get";
            }
            if (durationMethodMap[method]) {
                method = durationMethodMap[method];
            }
            if (params.as) {
                method = "as";
                methodArg = params.as;
            } else if (params.get) {
                method = "get";
                methodArg = params.get;
            }

            if (!durationObj[method]) {
                method = "humanize";
            }

            if (method === "humanize") {
                if (methodArg === undefined) {
                    methodArg = params.suffix;
                }
            }

            return durationObj[method](methodArg);
        });
    };

    var external = {
        /**
         * @method registerHelpers
         * @static
         * @param {object} hbars Handlebars instance
         * @description Register MomentHelper helpers with Handlebars
         *
         * - {@link template:moment}
         * - {@link template:duration}
         */
        registerHelpers: function (hbars) {
            MomentHelpers(hbars);
        }
    };

    return external;

}));

HandlebarsMoment.registerHelpers(Handlebars);


// moment helper not working with just a year
// calling moment() with just a year works in browser and server side (so daterange helper is not effected)
// ... so it seems to be the helper code, being buggy. quick fix is a wrapper:

Handlebars.registerHelper('momentWrapper', function (date, format) {
  return moment(date).format(format);
});


Handlebars.registerHelper('daterange', function (date_from, date_to) {

  var from = moment(date_from);
  var to = moment(date_to);

  var precision_from = date_from.split('-').length + '';
  var precision_to = date_to.split('-').length + '';

  var precision_max = Math.max(precision_from, precision_to);

  var formats_by_precision = {
    '1' : 'YYYY',
    '2' : 'MMM YYYY',
    '3' : 'D. MMM YYYY',
  };

  if(
    from.isSame(to, 'day') &&
    from.isSame(to, 'month') &&
    from.isSame(to, 'year')
  ) {

    return from.format( formats_by_precision[ precision_max ] );

  } else if(
    from.isSame(to, 'month') &&
    from.isSame(to, 'year') &&
    precision_max == 3
  ) {

    return from.format('D.') + '–' + to.format('D. MMM YYYY');

  } else if(
    from.isSame(to, 'year') &&
    precision_max == 2
  ) {

    return from.format('D. MMM') + ' – ' + to.format('D. MMM YYYY');

  } else {

    return from.format( formats_by_precision[ precision_from ] ) + ' – ' + to.format( formats_by_precision[ precision_to ] );

  }

});


/**
* Daterange helper to use in context of a calendar, where the start month and year
* are clear from the calendar layout
*/

Handlebars.registerHelper('daterangeCalendar', function (date_from, date_to) {

  var from = moment(date_from);
  var to = moment(date_to);

  if( from.isSame(to, 'month') && from.isSame(to, 'year') ) {

    return from.format('D.') + '–' + to.format('D. MMM');

  } else if( from.isSame(to, 'year') ) {

    return from.format('D. MMM') + ' – ' + to.format('D. MMM');

  } else {

    return from.format('D. MMM') + ' – ' + to.format('D. MMM YYYY');

  }

});

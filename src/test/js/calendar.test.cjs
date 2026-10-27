const {test}=require('node:test');
const assert=require('node:assert/strict');
const {categoryOf,filterEvents,formatEventRange}=require('../../main/resources/static/js/calendar.js');
test('existing event colors map to categories and filters do not mutate source data',()=>{
 const events=[{color:'#3788D8'},{color:'#28a745'},{color:'#dc3545'},{color:'#123456'}];
 assert.equal(categoryOf(events[0]),'mass');
 assert.equal(categoryOf({backgroundColor:'#28a745'}),'meeting');
 assert.equal(filterEvents(events,'all').length,4);
 assert.deepEqual(filterEvents(events,'other'),events.slice(2));
 assert.equal(filterEvents(events,'meeting').length,1);
 assert.equal(events.length,4);
});
test('single all-day event excludes the next-day endpoint from the displayed date',()=>{
 const result=formatEventRange({start:new Date(2026,9,27),end:new Date(2026,9,28),allDay:true});
 assert.match(result,/27일/); assert.doesNotMatch(result,/28일|~/); assert.match(result,/종일/);
});
test('multi-day all-day event shows its inclusive final day across month boundaries',()=>{
 const result=formatEventRange({start:new Date(2026,9,31),end:new Date(2026,10,3),allDay:true});
 assert.match(result,/10월 31일/); assert.match(result,/11월 2일/); assert.doesNotMatch(result,/11월 3일/);
});
test('timed events show start and end times with a new date for overnight events',()=>{
 const result=formatEventRange({start:new Date(2026,9,27,23,0),end:new Date(2026,9,28,1,0),allDay:false});
 assert.match(result,/27일/); assert.match(result,/28일/); assert.match(result,/~/);
});
test('events with no end or missing start have readable labels',()=>{
 assert.match(formatEventRange({start:new Date(2026,9,27),allDay:true}),/종일/);
 assert.doesNotMatch(formatEventRange({start:new Date(2026,9,27,10),allDay:false}),/~/);
 assert.equal(formatEventRange({}),'일정 시간이 지정되지 않았습니다.');
});

import java.time.*;
ZoneId z=ZoneId.of("Europe/Berlin"); LocalTime t=LocalTime.of(2,30);
// mirror of AlarmRecurrence.nextOccurrence for an every-day rule (java.time semantics only)
java.util.function.Function<ZonedDateTime,ZonedDateTime> next = after -> { for(int o=0;o<=7;o++){ ZonedDateTime c=after.toLocalDate().plusDays(o).atTime(t).atZone(after.getZone()); if(c.isAfter(after)) return c;} return null; };
ZonedDateTime a1=ZonedDateTime.of(2026,10,24,22,0,0,0,z); ZonedDateTime r1=next.apply(a1); System.out.println("fall-back first ring: "+r1+" = "+r1.toInstant());
ZonedDateTime afterRing=r1.plusMinutes(15); ZonedDateTime r2=next.apply(afterRing); System.out.println("after 15-min cap ("+afterRing+"): next "+r2);
ZonedDateTime repeated=ZonedDateTime.ofLocal(LocalDateTime.of(2026,10,25,2,40),z,ZoneOffset.ofHours(1)); System.out.println("from repeated hour "+repeated+": next "+next.apply(repeated));
ZonedDateTime s=ZonedDateTime.of(2027,3,27,22,0,0,0,z); System.out.println("spring-forward ring: "+next.apply(s));
/exit

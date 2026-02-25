package com.holyworld.autoreply.ai;

import com.holyworld.autoreply.HolyWorldAutoReply;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class ResponseEngine {

    private final ConcurrentHashMap<String, PlayerState> playerStates = new ConcurrentHashMap<>();
    private final List<ResponseRule> rules = new ArrayList<>();

    public ResponseEngine() {
        initializeRules();
        HolyWorldAutoReply.LOGGER.info("[Engine] Loaded {} rules", rules.size());
    }

    public static class PlayerState {
        public long checkStartTime;
        public int messageCount = 0;
        public boolean askedForAnydesk = false;
        public boolean gaveCodes = false;
        public boolean offeredConfession = false;
        public boolean mentionedRudesk = false;
        public boolean mentionedRustdesk = false;
        public String lastCategory = "";

        public PlayerState() {
            this.checkStartTime = System.currentTimeMillis();
        }

        public int getRemainingMinutes() {
            int elapsed = (int)((System.currentTimeMillis() - checkStartTime) / 60000);
            return Math.max(1, 7 - elapsed);
        }

        public long getElapsedMinutes() {
            return (System.currentTimeMillis() - checkStartTime) / 60000;
        }
    }

    @FunctionalInterface
    private interface Matcher {
        boolean test(String msg, String low, PlayerState st, String name);
    }

    @FunctionalInterface
    private interface Responder {
        String get(String msg, String low, PlayerState st, String name);
    }

    private static class ResponseRule {
        final String cat;
        final int pri;
        final Matcher matcher;
        final Responder responder;

        ResponseRule(String cat, int pri, Matcher m, Responder r) {
            this.cat = cat; this.pri = pri; this.matcher = m; this.responder = r;
        }
    }

    private static String pick(String... o) {
        return o[ThreadLocalRandom.current().nextInt(o.length)];
    }

    private static boolean has(String t, String... kw) {
        for (String k : kw) if (t.contains(k)) return true;
        return false;
    }

    private static boolean eq(String t, String... vals) {
        for (String v : vals) if (t.equals(v)) return true;
        return false;
    }

    private void initializeRules() {

        // 100: INSULTS
        rules.add(new ResponseRule("insult", 100,
            (m,l,s,n) -> has(l,
                "нахуй","нахуи","пошел нах","пошёл нах","иди нах",
                "хуй","хуи","хуе","хуё","хуесос","хуёсос",
                "ебал","ебан","ебат","ебу","ёба",
                "сука","суки","сучка","блядь","бляд",
                "далбаеб","долбаеб","долбоеб","дебил",
                "мразь","урод","гандон","гондон",
                "пидор","пидр","педик",
                "чмо","чмошник",
                "безмамн","мертвой мам","мёртвой мам",
                "твою мать","маму ебал","маме пизд",
                "пузо вырезал","сын бляд","сын свинь",
                "соси","саси","пизд"),
            (m,l,s,n) -> null));

        // 95: CONFESSION
        rules.add(new ResponseRule("confession", 95,
            (m,l,s,n) -> has(l,
                "я софт","я читер","я чит ","я читор",
                "я с софт","я с читами","я играю с чит",
                "у меня софт","у меня чит","у меня читы",
                "у меня x-ray","у меня xray",
                "я с модом","я с софтом",
                "я читер бб","я чит бань","я чит баньте",
                "у меня селестиал","у меня celestial",
                "с софтом бань","хочеш бань",
                "признание ентити","за хранения забань",
                "у меня всего x-ray",
                "все равно айпи сменю","всё равно на этот акк",
                "бань нахуй"),
            (m,l,s,n) -> null));

        // 94: LEAVE
        rules.add(new ResponseRule("leave", 94,
            (m,l,s,n) -> {
                String t = l.trim();
                return eq(t,"бб","bb") ||
                    has(l,"bb all","бб всем","all bb",
                        "лад баньте","ладно баньте","ладна банте",
                        "давай бан","я жду бан",
                        "качать не охота","качать не буду",
                        "не буду ничего скачивать","бб короче");
            },
            (m,l,s,n) -> {
                if (has(l,"удачи")) return "Спасибо за сотрудничество";
                return null;
            }));

        // 93: REFUSAL
        rules.add(new ResponseRule("refusal", 93,
            (m,l,s,n) -> has(l,"мне лень","забань на минимальн",
                "эту залупу","баньте","бань "),
            (m,l,s,n) -> null));

        // 92: SHORT CONFESSION
        rules.add(new ResponseRule("confess_short", 92,
            (m,l,s,n) -> {
                String t = l.trim();
                return eq(t,"признание","признаюсь","признаю","го признание") ||
                    has(l,"признаюсь что","я признаюсь","я признаюс",
                        "хорошо я признаюсь","ладно я софт");
            },
            (m,l,s,n) -> null));

        // 85: CODE
        rules.add(new ResponseRule("code", 85,
            (m,l,s,n) -> {
                String cleaned = m.replaceAll("[\\s\\-]", "");
                return cleaned.matches("\\d{6,10}");
            },
            (m,l,s,n) -> {
                s.gaveCodes = true;
                return pick("Принимай","+","Грузит","Ща подключусь");
            }));

        // 83: DISCORD
        rules.add(new ResponseRule("discord", 83,
            (m,l,s,n) -> has(l,
                "через дс","давай дс","дс можно","го дс","го в дс",
                "го через дс","можно дс","мб дс","по дс",
                "давай в дс","го по дс","давай по дс",
                "через дискорд","можно дискорд","го дискорд",
                "мой дс","могу дс","могу в дс",
                "go cheres ds","go w ds",
                "это дс","я дс кинул","дс пойти",
                "в звонок","пойдем в звонок",
                "го по диск","давай в дискорд"),
            (m,l,s,n) -> pick("-","По дс проверки не проводим","Скачивай аник")));

        // 82: VK TG
        rules.add(new ResponseRule("vk_tg", 82,
            (m,l,s,n) -> has(l,"через вк","го вк","можно вк",
                "через тг","го тг","тг можно","можно тг",
                "есть тг","есть вк","го по вк","демонстрация"),
            (m,l,s,n) -> pick("-","Скачивай аник")));

        // 81: LM
        rules.add(new ResponseRule("lm", 81,
            (m,l,s,n) -> has(l,"можно в лс","могу в лс","кому в лс"),
            (m,l,s,n) -> pick("Мне","Принимай")));

        // 80: GREETING
        rules.add(new ResponseRule("greeting", 80,
            (m,l,s,n) -> {
                if (s.messageCount > 3) return false;
                String t = l.trim();
                return has(t,"привет","прив","хай","здравств","приветик","прывект") ||
                    eq(t,"ку","qq","hi");
            },
            (m,l,s,n) -> {
                if (has(l,"привет я не читер"))
                    return pick("Привет давай аник","Привет скачивай анидеск");
                s.askedForAnydesk = true;
                return pick(
                    "Это проверка на читы, у Вас есть 7 мин. чтобы скинуть AnyDesk и пройти проверку! Признание уменьшает наказание! В случае отказа/выхода/игнора - Бан!",
                    "Привет! Жду аник",
                    "qq жду аник");
            }));

        // 78: REASON
        rules.add(new ResponseRule("reason", 78,
            (m,l,s,n) -> has(l,
                "за что","причина","за что прове",
                "почему вызвал","за что вызвал",
                "почему меня","что я сделал","что я зделал",
                "что случилось","а за что",
                "за что собственно","какая проверка",
                "я тока зашёл","я только зашел",
                "я возле дк","я просто игра",
                "я на спавне","в чем причина",
                "зачем вызвал","за что проверка",
                "а щас то за что","а чё это"),
            (m,l,s,n) -> {
                if (has(l,"причина","в чем причина"))
                    return pick("Многочисленные репорты","Репорты",
                        "Модератор в праве не разглашать причину проверки игроку");
                return pick("За все хорошее","Репорты","Надо","Многочисленные репорты");
            }));

        // 77: NOT CHEATER
        rules.add(new ResponseRule("not_cheater", 77,
            (m,l,s,n) -> has(l,
                "я не читер","я не читар","я не софт",
                "я чист","у меня нет читов","у меня нету читов",
                "без читов","без софта","я ансофт",
                "я 100%","я не использую","я легит",
                "я готов пройти"),
            (m,l,s,n) -> pick("Скачивай аник","Верю скачивай","Аник жду","Ну я жду")));

        // 75: WHAT IS ANYDESK
        rules.add(new ResponseRule("what_anydesk", 75,
            (m,l,s,n) -> has(l,
                "что за аник","что такое аник","что за анидеск",
                "что такое анидеск","что это за прог",
                "что за прога","че за прога",
                "типо ты в моем","будешь лазать","управлять моим",
                "анидеск это что"),
            (m,l,s,n) -> {
                if (has(l,"типо ты в моем","будешь лазать","управлять моим")) return "+";
                return pick("Программа удаленного доступа","Удаленный доступ","Программа такая");
            }));

        // 74: DOWNLOADING
        rules.add(new ResponseRule("downloading", 74,
            (m,l,s,n) -> has(l,
                "скачиваю","скачиваеться","скачивается",
                "качаю","качается","загружается","грузит",
                "устанавливаю","устанавливается",
                "пачти скачался","почти скачал",
                "немного осталось","щас скачаю","ща скачаю",
                "загрузил","скачал","жди качаю",
                "ок скачаю","я качаю"),
            (m,l,s,n) -> {
                if (has(l,"скачал","загрузил","скачался"))
                    return pick("Кидай код","Кидай длинный код","Открывай его");
                return pick("Жду",s.getRemainingMinutes()+" min","Жду жду","Время идет");
            }));

        // 73: CANT DOWNLOAD
        rules.add(new ResponseRule("cant_dl", 73,
            (m,l,s,n) -> has(l,
                "не скачивается","не качается","не загружается",
                "не грузит","не могу скачать",
                "не работает","не робит","ошибка",
                "вирус","трояны","не дает скачать",
                "не запускается","немагу",
                "сайт не грузит","не открывается",
                "виндоус","антивирус","бяка",
                "у меня не работает","у меня ошибка"),
            (m,l,s,n) -> {
                if (!s.mentionedRudesk) {
                    s.mentionedRudesk = true;
                    return pick("Скачивай RuDeskTop","Качай рудеск","Газуй рудеск");
                }
                if (!s.mentionedRustdesk) {
                    s.mentionedRustdesk = true;
                    return pick("Скачивай RustDesk","Качай RustDesk");
                }
                return "Все должно работать";
            }));

        // 72: NO ANYDESK
        rules.add(new ResponseRule("no_anik", 72,
            (m,l,s,n) -> has(l,
                "нету аник","нет аник","у меня нету ани",
                "аника нет","анидеска нет",
                "нету такого","нету его",
                "у меня нету","нету программы",
                "просто нету","тут анидеска нет"),
            (m,l,s,n) -> pick("Скачивай","Скачивай анидеск","Качай")));

        // 71: RUDESK
        rules.add(new ResponseRule("rudesk", 71,
            (m,l,s,n) -> has(l,
                "рудеск","rudesk","rudesktop","рудесктоп",
                "рудекс","рудекстор",
                "можно по рудеск","рудеск сойдет",
                "а рудеск не подойдет"),
            (m,l,s,n) -> {
                s.mentionedRudesk = true;
                if (has(l,"можно","подойдет","сойдет")) return pick("+","Газуй","Да");
                return pick("+","Газуй","Скачивай");
            }));

        // 70: RUSTDESK
        rules.add(new ResponseRule("rustdesk", 70,
            (m,l,s,n) -> has(l,"растдеск","растдекс","раст деск","rustdesk","rust desk"),
            (m,l,s,n) -> {
                s.mentionedRustdesk = true;
                if (has(l,"можно","подойдет","могу")) return pick("+","Да");
                return "+";
            }));

        // 69: WHERE DOWNLOAD
        rules.add(new ResponseRule("where_dl", 69,
            (m,l,s,n) -> has(l,
                "где скачать","как скачать","хз как скачать",
                "с какого сайта","какая ссылка",
                "что скачать","что качать",
                "а что надо скачать","название ани",
                "а где код","где код найти","скинь ссылку"),
            (m,l,s,n) -> {
                if (has(l,"код","где код"))
                    return pick("При запуске сразу будет","Прямо на самом видном месте");
                if (has(l,"название")) return "Да";
                return pick("anydesk com","В гугле пиши анидеск","Инструкция в чате",
                    "В браузере пиши anydesk com");
            }));

        // 68: PHONE
        rules.add(new ResponseRule("phone", 68,
            (m,l,s,n) -> has(l,"я с телефон","с телефона","на телефоне",
                "с мобильн","на андроид"),
            (m,l,s,n) -> pick("Скачивай аник на телефон","Вообще не волнует")));

        // 67: WHAT NEXT
        rules.add(new ResponseRule("what_next", 67,
            (m,l,s,n) -> has(l,
                "что дальше","чё дальше","что делать",
                "чё делать","что мне делать","чо делать",
                "что скидывать","что нужно делать",
                "как мне пройти","куда жмать",
                "я не понимаю","что мне надо делать"),
            (m,l,s,n) -> {
                if (s.gaveCodes) return pick("Принимай","Принять нажми");
                if (s.askedForAnydesk) return pick("Кидай код","Кидай длинный код");
                return pick("Скачивай анидеск","Все инструкции в чате","Инструкция в лс");
            }));

        // 66: TIME
        rules.add(new ResponseRule("time", 66,
            (m,l,s,n) -> has(l,
                "скок времени","сколько времени","скок время",
                "скок минут","сколько минут","скок у меня",
                "сколько у меня","сколько ещё","сколько еще",
                "сколько осталось","скок осталось",
                "доп время","продли время","дай время",
                "можно доп","можно подождать"),
            (m,l,s,n) -> {
                if (has(l,"доп","продли","подождать")) return "-";
                return s.getRemainingMinutes()+" min";
            }));

        // 65: WAIT
        rules.add(new ResponseRule("wait", 65,
            (m,l,s,n) -> {
                String t = l.trim();
                return eq(t,"ща","щас","сек","секу","щя","щяс","шас") ||
                    has(l,"подожд","погод","чуть чуть","жди","щаща","щас сек","ок щас","ша сек");
            },
            (m,l,s,n) -> pick("Жду",s.getRemainingMinutes()+" минут","+","Давай")));

        // 64: CONFESSION Q
        rules.add(new ResponseRule("conf_q", 64,
            (m,l,s,n) -> has(l,"какое признание","что за признание",
                "на скок меньше","на сколько забаните","сколько бан","а скок целый"),
            (m,l,s,n) -> {
                s.offeredConfession = true;
                if (has(l,"какое")) return "Признание в читах";
                return pick("Признание 20 дней, отказ 30 дней","30 дней");
            }));

        // 63: ACCEPT
        rules.add(new ResponseRule("accept", 63,
            (m,l,s,n) -> has(l,"принял","я принял","как принять",
                "приинимать","нет кнопки","не пришло","от имени","от кого"),
            (m,l,s,n) -> {
                if (has(l,"как принять","нет кнопки")) return "Нажми кнопку принять";
                if (has(l,"от имени","от кого")) return "Любой";
                if (has(l,"не пришло")) return "Принимай";
                return pick("+","Пред 1/3 не трогай мышку");
            }));

        // 62: REG
        rules.add(new ResponseRule("reg", 62,
            (m,l,s,n) -> has(l,"регаюсь","регаться","регистрац","зарегаю"),
            (m,l,s,n) -> "Не надо там регаться"));

        // 61: MINIMAP
        rules.add(new ResponseRule("minimap", 61,
            (m,l,s,n) -> has(l,"миникарта","минимап","пульс это"),
            (m,l,s,n) -> {
                if (has(l,"офиц")) return "Не не софт";
                if (has(l,"пульс")) return "Смотря какой";
                return "+";
            }));

        // 60: REPORT
        rules.add(new ResponseRule("report", 60,
            (m,l,s,n) -> has(l,"тут один читер","тут читер",
                "могу дать его ник","против меня софтер"),
            (m,l,s,n) -> pick("Давай","Напиши /cr nik")));

        // 59: RESOURCES
        rules.add(new ResponseRule("resources", 59,
            (m,l,s,n) -> has(l,"можно ресы","ресы раздам",
                "можно баблко","деньги отдам","тимейту деньги","можно кинуть"),
            (m,l,s,n) -> "-"));

        // 58: LEGAL
        rules.add(new ResponseRule("legal", 58,
            (m,l,s,n) -> has(l,"не законно","незаконно","не доверяю","родительский контроль"),
            (m,l,s,n) -> {
                if (has(l,"родительский")) return "Скачивай анидеск проси разрешения";
                return "Заходя на сервер вы соглашаетесь с правилами и при проверке вы обязаны предоставить анидеск";
            }));

        // 57: FROM RF
        rules.add(new ResponseRule("rf", 57,
            (m,l,s,n) -> has(l,"я из рф","я с рф","из рф","с рф","из россии",
                "аник не ворк на территории"),
            (m,l,s,n) -> {
                s.mentionedRudesk = true;
                return pick("Скачивай RuDeskTop","Скачивай рудеск",
                    "Скачивай RuDeskTop или запускай впн на пк и с впн аник включай");
            }));

        // 56: VPN
        rules.add(new ResponseRule("vpn", 56,
            (m,l,s,n) -> has(l,"впн","vpn","кикнет"),
            (m,l,s,n) -> "Скачивай RuDeskTop значит"));

        // 55: PREV CHECK
        rules.add(new ResponseRule("prev_check", 55,
            (m,l,s,n) -> has(l,"меня проверяли","уже проверяли",
                "вчера проверял","проверяли сегодня","я вчера прову"),
            (m,l,s,n) -> {
                if (has(l,"вчера")) return "Обманывать не хорошо";
                return pick("Я тебя еще раз проверю","Аник жду");
            }));

        // 54: PAID
        rules.add(new ResponseRule("paid", 54,
            (m,l,s,n) -> has(l,"платная","платный","евро надо","бесплатн"),
            (m,l,s,n) -> pick("Она не платная","Он бесплатный",
                "Заходишь на сайт anydesk com для домашнего использования")));

        // 52: YES
        rules.add(new ResponseRule("yes", 52,
            (m,l,s,n) -> {
                String t = l.trim();
                return eq(t,"да","да?","+","ок","окей","ладно",
                    "хорошо","понял","пон","угу","ну","ага",
                    "da","ladno","ну ок","тогда ок");
            },
            (m,l,s,n) -> {
                if (!s.askedForAnydesk) {
                    s.askedForAnydesk = true;
                    return s.getRemainingMinutes()+" min у тебя";
                }
                if (s.gaveCodes) return pick("Принимай","+");
                return pick("Жду","+","Давай");
            }));

        // 51: QUESTION MARKS
        rules.add(new ResponseRule("qmarks", 51,
            (m,l,s,n) -> l.trim().matches("\\?+"),
            (m,l,s,n) -> {
                if (s.messageCount <= 2) return "Проверка";
                return pick("Аник жду","Жду","Скачивай аник");
            }));

        // 50: SHORT
        rules.add(new ResponseRule("short", 50,
            (m,l,s,n) -> {
                String t = l.trim();
                return eq(t,"аник","аник?","кидай",
                    "ну че","ну чо","го","go",
                    "вот","на","это?",
                    "ало","ау","аууу","але",
                    "модер","ты тут","ты тут?",
                    "ты здесь","ты здесь?");
            },
            (m,l,s,n) -> {
                String t = l.trim();
                if (has(t,"аник")) return pick("+","Жду код");
                if (has(t,"кидай")) return pick("Ты из рф?","Кидай код");
                if (has(t,"вот","на","это")) return pick("+","Принимай");
                if (has(t,"ты тут","ты здесь","ало","ау","модер","але"))
                    return pick("Да да я тут","+","Я тут");
                return pick("Жду аник","+","Аник жду");
            }));

        // 48: WEAK PC
        rules.add(new ResponseRule("weak_pc", 48,
            (m,l,s,n) -> has(l,"пк слаб","комп слаб","интернет слаб",
                "инет говно","пк за 15к","медленно","лагает"),
            (m,l,s,n) -> pick("Жду",s.getRemainingMinutes()+" минут","Скачивай")));

        // 46: EMOTIONAL
        rules.add(new ResponseRule("emotional", 46,
            (m,l,s,n) -> {
                String t = l.trim();
                return t.matches("[)(]+") ||
                    eq(t,"хаха","хахаха","ахахах","xd","найс","круто","прикольно","гг","лол");
            },
            (m,l,s,n) -> {
                if (has(l,"хаха","ахах","xd")) return "После проверки)";
                return pick("Аник жду","Признание уменьшает срок на 35%");
            }));

        // 45: STALLING
        rules.add(new ResponseRule("stalling", 45,
            (m,l,s,n) -> has(l,"я в дубае","расказу","поговорим",
                "забаниш я ночь","в подушку плакать","мне пизда","я девка"),
            (m,l,s,n) -> {
                if (has(l,"мне пизда")) {
                    s.offeredConfession = true;
                    return "Признание уменьшает бан на 35%";
                }
                return pick("Аник жду","Бро не тяни время","Аник или признание");
            }));

        // 44: AUTO CONFESSION
        rules.add(new ResponseRule("auto_conf", 44,
            (m,l,s,n) -> s.messageCount > 5 && !s.offeredConfession && s.getElapsedMinutes() >= 3,
            (m,l,s,n) -> {
                s.offeredConfession = true;
                return pick("Признание уменьшает наказание на 35%",
                    "Признание уменьшает срок бана на 35%",
                    "Давай что бы время не тратить ты признаешься и я забаню на 35% меньше");
            }));

        // 43: NO
        rules.add(new ResponseRule("no", 43,
            (m,l,s,n) -> {
                String t = l.trim();
                return eq(t,"нет","не","неа","нее") || t.startsWith("нееее");
            },
            (m,l,s,n) -> pick("Тогда жду аник","Скачивай аник","Аник жду")));

        // 40: TRANSLIT
        rules.add(new ResponseRule("translit", 40,
            (m,l,s,n) -> has(l,"vse bani","i skacat ne mogy","togda idi v pizdy"),
            (m,l,s,n) -> {
                if (has(l,"vse bani","pizdy")) return null;
                return "Ты сможешь я в тебя верю";
            }));

        // 38: CONNECTION
        rules.add(new ResponseRule("conn", 38,
            (m,l,s,n) -> has(l,"клиент не в сети","не подключается",
                "соединение заверш","не воркает","кинь еще раз"),
            (m,l,s,n) -> pick("Скачивай RustDesk","Переприми","Переустанови аник")));

        // 35: ENGLISH
        rules.add(new ResponseRule("english", 35,
            (m,l,s,n) -> has(l,"всё на англ","все на англ","на английском"),
            (m,l,s,n) -> "У тебя "+s.getRemainingMinutes()+" минут осталось"));

        // 30: PLUGIN
        rules.add(new ResponseRule("plugin", 30,
            (m,l,s,n) -> has(l,"плагин","plugin","ad1","три линии","полный доступ"),
            (m,l,s,n) -> "Нажми слева сверху на три линии в anydesk, настройкА, Плагин AD1, Активировать!"));

        // 25: DONE
        rules.add(new ResponseRule("done", 25,
            (m,l,s,n) -> has(l,"я прошел","я прошёл","спасибо","спс"),
            (m,l,s,n) -> pick("Рад помочь","Пред 1/3 не трогай мышку","+")));

        // 20: TRYING
        rules.add(new ResponseRule("trying", 20,
            (m,l,s,n) -> has(l,"попробую","постараюсь","я тут",
                "запускаю","открыл","открываю","лан"),
            (m,l,s,n) -> pick("Жду",s.getRemainingMinutes()+" минут","+","Давай")));

        // 10: CATCHALL
        rules.add(new ResponseRule("catchall", 10,
            (m,l,s,n) -> true,
            (m,l,s,n) -> {
                if (s.messageCount <= 1) {
                    s.askedForAnydesk = true;
                    return "Это проверка на читы, у Вас есть 7 мин. чтобы скинуть AnyDesk и пройти проверку! Признание уменьшает наказание!";
                }
                if (s.messageCount > 8 && !s.offeredConfession) {
                    s.offeredConfession = true;
                    return "Признание уменьшает наказание на 35%";
                }
                return pick("Аник жду","Скачивай аник","Жду анидеск","Анидеск жду");
            }));

        rules.sort((a,b) -> Integer.compare(b.pri, a.pri));
    }

    public String getResponse(String playerMessage, String playerName) {
        if (playerMessage == null || playerMessage.trim().isEmpty()) return null;

        String lower = playerMessage.toLowerCase().trim();
        PlayerState state = playerStates.computeIfAbsent(playerName, k -> new PlayerState());
        state.messageCount++;

        for (ResponseRule rule : rules) {
            try {
                if (rule.matcher.test(playerMessage, lower, state, playerName)) {
                    String response = rule.responder.get(playerMessage, lower, state, playerName);
                    state.lastCategory = rule.cat;
                    if (response == null) {
                        HolyWorldAutoReply.LOGGER.info("[Engine] BAN [{}] {}: {}",
                            rule.cat, playerName, playerMessage);
                    } else {
                        HolyWorldAutoReply.LOGGER.info("[Engine] [{}] '{}' -> '{}'",
                            rule.cat, playerMessage, response);
                    }
                    return response;
                }
            } catch (Exception e) {
                HolyWorldAutoReply.LOGGER.error("[Engine] Error {}: {}", rule.cat, e.getMessage());
            }
        }
        return null;
    }

    public void clearPlayerState(String n) { playerStates.remove(n); }
    public void clearAllStates() { playerStates.clear(); }
}

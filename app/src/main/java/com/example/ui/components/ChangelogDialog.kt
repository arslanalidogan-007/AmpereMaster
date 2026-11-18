package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localization.AppLanguage
import com.example.localization.LocalAppLanguage
import com.example.localization.LocalAppStrings
import com.example.ui.theme.ChargingCyan
import com.example.ui.theme.ElectricGreen

/**
 * Dialog zur Anzeige der Versionshistorie und Feature-Updates.
 */
@Composable
fun ChangelogDialog(
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val currentLang = LocalAppLanguage.current
    val scrollState = rememberScrollState()

    val (currentBadge, prevBadge) = when (currentLang) {
        AppLanguage.DE -> "Aktuell" to "Vorherige Version"
        AppLanguage.EN -> "Latest" to "Previous"
        AppLanguage.TR -> "Güncel" to "Önceki"
        AppLanguage.KU -> "Niha" to "Beriya vê"
        AppLanguage.AR -> "الحالي" to "السابق"
        AppLanguage.FR -> "Actuel" to "Précédente"
        AppLanguage.ES -> "Actual" to "Anterior"
        AppLanguage.RU -> "Текущая" to "Предыдущая"
        AppLanguage.ZH -> "最新" to "历史版本"
        AppLanguage.HI -> "नवीनतम" to "पिछला"
    }

    val features104 = when (currentLang) {
        AppLanguage.DE -> listOf(
            "Netzteil- & Kabel-Datenbank (Saved Chargers): Getestete Netzteile/Kabel mit Güteklasse speichern und Ladesitzungen direkt zuordnen",
            "100%-Lokalisierung & FileProvider-CSV: Alle UI-, Widget- & Benachrichtigungstexte vollständig in 10 Sprachen; CSV-Export als echte Datei",
            "Dual-Cell-Kalibrierung (2S) & Verbrauchs-Korrelation: Optionaler 2×-Faktor für 65W–120W+ Dual-Cell-Geräte sowie Display-Ein vs. Standby-Rate",
            "Adaptives Tablet-/Foldable-UI & SOH-Widget: Automatische NavigationRail + Split-Ansicht auf großen Screens & konfigurierbare Widget-SOH-Anzeige",
            "Nicht-destruktive Room-Migration (v3 → v4): Bestehende Ladekurven und SOH-Historien bleiben bei Updates erhalten"
        )
        AppLanguage.EN -> listOf(
            "Saved Chargers & Cables Database: Store benchmarked chargers with quality grades and tag charging sessions directly",
            "100% Localization & FileProvider CSV Export: All remaining UI, widget & service notifications translated in 10 languages; genuine .csv file sharing",
            "Dual-Cell Calibration (2S) & Drain Correlation: Optional 2× scaling for 65W–120W+ dual-cell devices plus Screen-On vs. Standby drain breakdown",
            "Adaptive Tablet/Foldable UI & Widget Mode: Automatic NavigationRail + split dashboard on wide displays and toggleable SOH widget readout",
            "Non-destructive Room Migration (v3 → v4): Preserves all recorded curves and SOH snapshots across updates"
        )
        AppLanguage.TR -> listOf(
            "Kayıtlı Şarj Aleti & Kablo Veritabanı: Test edilen adaptörleri/kabloları kalite puanıyla kaydetme ve oturumlara atama",
            "%100 Yerelleştirme & Dosya CSV Dışa Aktarma: Tüm arayüz, widget ve servis bildirimleri 10 dilde eksiksiz; gerçek .csv dosya paylaşımı",
            "Çift Hücre Kalibrasyonu (2S) & Tüketim Analizi: 65W–120W+ cihazlar için 2× seçeneği ve ekran açık/bekleme tüketim oranı",
            "Uyarlanabilir Tablet Arayüzü & SOH Widget Modu: Geniş ekranlarda NavigationRail + bölünmüş düzen ve widget detay ayarı",
            "Kayıpsız Veritabanı Geçişi (v3 → v4): Güncellemelerde mevcut SOH geçmişi ve şarj eğrileri korunur"
        )
        AppLanguage.KU -> listOf(
            "Danegîra Şarjker & Kabloyan (Saved Chargers): Profîlên şarjkeran bi pileya kalîteyê tomar bike û bi rûniştinan ve girê bide",
            "Wergera 100% & Derxistina Pelê CSV: Hemû nivîsên UI, widget û xizmetê bi 10 zimanan; parvekirina pelê .csv ya rastîn",
            "Kalîbrasyona Du-Hucreyî (2S) & Analîza Xercê: Faktora 2× ji bo 65W–120W+ û berawirdkirina Ekran Vekirî vs. Bêdengî",
            "Navrûya Tabletê & Widgeta SOH: Li ser ekranên fireh NavigationRail + nîşandana SOH li ser widgetê",
            "Koçberiya Room bêyî windakirina daneyan (v3 → v4): Dîroka SOH û xêzên berê tên parastin"
        )
        AppLanguage.AR -> listOf(
            "قاعدة بيانات الشواحن والكابلات: حفظ نتائج اختبارات الشواحن مع تصنيف الجودة وربطها بجلسات الشحن",
            "ترجمة 100% وتصدير CSV عبر FileProvider: تعريب شامل لكل التنبيهات والأداة المصغرة مع مشاركة ملف .csv فعلي",
            "معايرة الخلايا المزدوجة (2S) وتحليل الاستهلاك: دعم معامل 2× لأجهزة 65W–120W+ ومقارنة استهلاك الشاشة مع وضع الانتظار",
            "واجهة متجاوبة للأجهزة اللوحية وأداة SOH: شريط تنقل جانبي تلقائي للشاشات الكبيرة وعرض حالة SOH على الويدجت",
            "ترقية قاعدة البيانات دون فقدان البيانات (v3 → v4): الحفاظ على سجل المنحنيات وتاريخ السعة"
        )
        AppLanguage.FR -> listOf(
            "Base de Chargeurs & Câbles : Enregistrement des profils testés avec note de qualité et attribution aux sessions",
            "Localisation 100% & Export Fichier CSV : Tous les textes d'interface, widgets et notifications traduits ; partage de vrai fichier .csv",
            "Calibration Double Cellule (2S) & Analyse Écran : Facteur 2× pour appareils 65W–120W+ et taux Écran Allumé vs. Veille",
            "Interface Tablette Adaptative & Widget SOH : NavigationRail automatique sur grand écran et mode détaillé pour le widget",
            "Migration Room sans perte (v3 → v4) : Conservation de l'historique SOH et des courbes lors des mises à jour"
        )
        AppLanguage.ES -> listOf(
            "Base de Cargadores y Cables: Guarda perfiles probados con calificación y asígnalos directamente a cada sesión",
            "Localización 100% y Exportación CSV Real: Interfaz, widget y notificaciones en 10 idiomas; compartir archivo .csv vía FileProvider",
            "Calibración Doble Celda (2S) y Consumo Pantalla: Factor 2× para 65W–120W+ y desglose de descarga Pantalla Activa vs. Reposo",
            "Diseño Adaptativo Tablet y Widget SOH: NavigationRail automática en pantallas grandes y detalle SOH configurable en el widget",
            "Migración Room Segura (v3 → v4): Conserva todo tu historial de salud SOH y curvas tras actualizar"
        )
        AppLanguage.RU -> listOf(
            "База зарядных устройств и кабелей: Сохранение протестированных ЗУ с рейтингом и привязка к сессиям зарядки",
            "100% локализация и экспорт файла CSV: Полный перевод уведомлений и виджета на 10 языков + отправка реального .csv файла",
            "Калибровка Dual-Cell (2S) и анализ разряда: Множитель 2× для 65W–120W+ и сравнение расхода экрана и режима ожидания",
            "Адаптивный интерфейс для планшетов и виджет SOH: Боковая панель NavigationRail и настройка деталей SOH на виджете",
            "Безопасная миграция БД (v3 → v4): Сохраняет всю историю графиков и снимков SOH при обновлении"
        )
        AppLanguage.ZH -> listOf(
            "充电器与线缆档案库：将实测充电器评分与功率存入数据库，一键关联到各次充电会话进行对比",
            "100% 多语言覆盖与真实 CSV 文件导出：补全所有通知、桌面小组件与界面文本的 10 国语言翻译，支持 FileProvider 导出",
            "双串联电芯校准 (2S) 与亮屏耗电分析：为 65W–120W+ 机型提供 2× 倍率切换，新增亮屏与息屏待机耗电拆解",
            "平板/折叠屏自适应布局与小组件详情：大屏自动启用侧边 NavigationRail 与双栏视图，小组件支持 SOH 显示开关",
            "无损数据库升级迁移 (v3 → v4)：完整保留过往充放电曲线及长期 SOH 健康记录"
        )
        AppLanguage.HI -> listOf(
            "सहेजे गए चार्जर और केबल डेटाबेस: बेंचमार्क प्रोफ़ाइल को ग्रेड के साथ सहेजें और चार्जिंग सत्रों से जोड़ें",
            "100% अनुवाद और असली CSV फ़ाइल निर्यात: सभी सूचनाएं, विजेट और UI 10 भाषाओं में; FileProvider से .csv शेयर",
            "डुअल-सेल कैलिब्रेशन (2S) और डिस्चार्ज विश्लेषण: 65W–120W+ हेतु 2× स्केलिंग और स्क्रीन-ऑन बनाम स्टैंडबाय दर",
            "अडैप्टिव टैबलेट/फ़ोल्डेबल लेआउट और SOH विजेट: बड़ी स्क्रीन पर NavigationRail और विजेट में SOH प्रदर्शन विकल्प",
            "सुरक्षित डेटाबेस माइग्रेशन (v3 → v4): अपडेट के बाद भी पूरा चार्जिंग और SOH इतिहास सुरक्षित रहता है"
        )
    }

    val features103 = when (currentLang) {
        AppLanguage.DE -> listOf(
            "Interaktiver Kurven-Zoom & Vergleich: Stufenloser Pinch-to-Zoom (1×–8×), horizontales Panning & gleichzeitige Überlagerung zweier Ladesitzungen",
            "SOH-Alterungstracker (State of Health): Langzeiterfassung der realen Restkapazität in mAh gegen die Nennkapazität mit Mehrwochen-Diagramm & Kalibrierung",
            "Netzteil- & Kabel-Gütebewertung: Automatische Live-Benotung (Klasse A+ bis D) sowie interaktiver 30-Sekunden-Stresstest für Maximalstrom & Spannungsstabilität"
        )
        AppLanguage.EN -> listOf(
            "Interactive Curve Zoom & Compare: Smooth pinch-to-zoom (1×–8×), horizontal panning & dual charging session overlay comparison",
            "Battery SOH Aging Tracker: Automatic remaining capacity estimation (mAh vs. design capacity) with multi-week trend chart & calibration",
            "Charger & Cable Quality Benchmark: Live efficiency grading (Class A+ to D) plus 30-second stress test for peak current & voltage stability"
        )
        AppLanguage.TR -> listOf(
            "Etkileşimli Grafik Yakınlaştırma & Karşılaştırma: 1×–8× kıstırma yakınlaştırması, yatay kaydırma ve iki şarj oturumunu üst üste kıyaslama",
            "Pil SOH Yaşlanma Takibi: Gerçek kalan kapasitenin (mAh / fabrika değeri) çok haftalı trend grafiği ve kalibrasyonu",
            "Şarj Aleti & Kablo Kalite Puanı: Canlı verimlilik sınıfı (A+ – D) ve maksimum kararlı akım için 30 saniyelik stres testi"
        )
        AppLanguage.KU -> listOf(
            "Mezinkirina Grafikê & Berawirdkirin: Pinch-to-Zoom (1×–8×), şemitandina horizontî û berawirdkirina du rûniştinên barkirinê",
            "Şopandina Tenduristiya Pîlê (SOH): Tomarkirina kapasîteya rastîn bi mAh û grafîka guhertinê li ser hefteyan û mehan",
            "Nirxandina Kalîteya Şarjker & Kabloyê: Pîvana karîgeriyê (A+ – D) û testa stresê ya 30 çirkeyan ji bo herikîna herî zêde"
        )
        AppLanguage.AR -> listOf(
            "تكبير المنحنى والمقارنة التفاعلية: تكبير بالأصابع (1×–8×)، تحريك أفقي، ومقارنة جلستي شحن متراكبتين مباشرة",
            "متتبع صحة البطارية (SOH): تقدير السعة الفعلية المتبقية بالملي أمبير مع رسم بياني طويل المدى ومعايرة السعة",
            "تقييم جودة الشاحن والكابل: تصنيف فوري للكفاءة (فئة A+ إلى D) واختبار إجهاد لمدة 30 ثانية لقياس ثبات الجهد والتيار"
        )
        AppLanguage.FR -> listOf(
            "Zoom & Comparaison de Courbes : Pinch-to-Zoom (1×–8×), déplacement horizontal et superposition directe de deux sessions de charge",
            "Suivi du Vieillissement SOH : Estimation de la capacité réelle restante (mAh) avec graphique multi-semaines et calibrage",
            "Évaluation Chargeur & Câble : Notation en direct (Classe A+ à D) et test de stress de 30 s pour courant maximal et stabilité"
        )
        AppLanguage.ES -> listOf(
            "Zoom y Comparación Interactiva: Pellizcar para hacer zoom (1×–8×), desplazamiento horizontal y superposición directa de dos sesiones",
            "Rastreador de Salud SOH: Estimación automática de capacidad real (mAh) con gráfico de tendencia de semanas y calibración",
            "Evaluación de Cargador y Cable: Calificación en vivo (Clase A+ a D) y prueba de estrés de 30 s para corriente máxima y estabilidad"
        )
        AppLanguage.RU -> listOf(
            "Масштабирование и сравнение кривых: Pinch-to-Zoom (1×–8×), панорамирование и наложение двух зарядных сессий друг на друга",
            "Трекер износа аккумулятора (SOH): Автоматический расчет остаточной емкости (мАч), калибровка и график за недели/месяцы",
            "Оценка качества ЗУ и кабеля: Живой рейтинг эффективности (Класс A+ – D) и 30-секундный стресс-тест стабильности"
        )
        AppLanguage.ZH -> listOf(
            "交互式曲线缩放与多会话对比：双指捏合缩放 (1×–8×)、横向拖动浏览及双充电曲线叠加对比",
            "电池 SOH 老化追踪器：自动估算真实剩余容量 (mAh 与设计容量对比)，支持跨周/月趋势图表及手动校准",
            "充电器与线缆品质评分：实时效率评级 (A+ 至 D 级) 与 30 秒高负载压力测试，检测最大稳定电流与压降"
        )
        AppLanguage.HI -> listOf(
            "इंटरैक्टिव कर्व ज़ूम और तुलना: पिंच-टू-ज़ूम (1×–8×), हॉरिजॉन्टल पैन और दो चार्जिंग सत्रों की सीधी ओवरले तुलना",
            "बैटरी SOH एजिंग ट्रैकर: वास्तविक शेष क्षमता (mAh बनाम डिज़ाइन क्षमता) का बहु-सप्ताह चार्ट और कैलिब्रेशन",
            "चार्जर और केबल गुणवत्ता बेंचमार्क: लाइव दक्षता ग्रेडिंग (क्लास A+ से D) और अधिकतम स्थिर करंट हेतु 30-सेकंड स्ट्रेस टेस्ट"
        )
    }

    val features102 = when (currentLang) {
        AppLanguage.DE -> listOf(
            "Akku-Überhitzungsschutz: Konfigurierbarer Temperaturalarm (38°C–52°C) mit akustischer & haptischer Warnung",
            "Ladeabschluss-Zusammenfassung: Automatische Auswertung beim Abstecken (Dauer, geladene %, Watt, Energie in mAh)",
            "Ladeprotokoll-Erkennung: Live-Identifikation von USB-PD, PPS, Fast Charge, Standard USB und Wireless Qi",
            "Akkuschonungs-Rechner: Visualisierung der vermiedenen Alterungszyklen und thermischen Belastung",
            "Standby-Entladerate & CSV-Export: Messung des Verbrauchs pro Stunde (%/h) und Datenexport für Excel & Tabellen",
            "AmpereMaster Home-Screen Widget: Schnelle Echtzeit-Akkustands- und Leistungsanzeige auf dem Startbildschirm"
        )
        AppLanguage.EN -> listOf(
            "Thermal Protection: Configurable battery overheat alarm (38°C–52°C) with audio & haptic alerts",
            "Charging Session Recap: Automatic summary upon unplugging (duration, added %, peak power, energy in mAh)",
            "Charging Protocol Detection: Live identification of USB-PD, PPS, Fast Charge, Standard USB, and Qi",
            "Battery Guard Calculator: Visual estimation of avoided wear cycles and thermal stress benefits",
            "Standby Drain Rate & CSV Export: Real-time hourly discharge (%/hr) and metric export for spreadsheets",
            "AmpereMaster Home Screen Widget: Instant live battery level, wattage, and status on your home screen"
        )
        AppLanguage.TR -> listOf(
            "Aşırı Isınma Koruması: Ayarlanabilir sıcaklık alarmı (38°C–52°C) ve titreşimli/sesli uyarı",
            "Şarj Oturumu Özeti: Kablo çıkarıldığında otomatik özet (süre, eklenen %, maks. güç, mAh enerjisi)",
            "Şarj Protokolü Tespiti: USB-PD, PPS, Hızlı Şarj, Standart USB ve Qi kablosuz şarj algılama",
            "Pil Tasarrufu Hesaplayıcı: Korunan döngü ömrü ve termal stres analiz göstergesi",
            "Bekleme Tüketimi & CSV Dışa Aktarma: Saatlik boşalma oranı (%/saat) ve Excel için veri paylaşımı",
            "AmpereMaster Ana Ekran Widget'ı: Ana ekranda canlı pil yüzdesi, güç ve sıcaklık gösterimi"
        )
        AppLanguage.KU -> listOf(
            "Parastina Germahiya Zêde: Alarma germahiya pîlê (38°C–52°C) bi hişyariya deng û lerzînê",
            "Kurteya Barkirinê: Piştî veqetandina kabloyê raporek xweser (dem, rêjeya zêdebûyî, hêz, mAh)",
            "Naskirina Protokola Barkirinê: Tespîtkirina zindî ya USB-PD, PPS, Fast Charge û Qi ya bêtêl",
            "Hesabkera Parastina Pîlê: Nîşandana dewrên parastî û stresa germî ya kêmkirî",
            "Rêjeya Xilasbûnê & Derxistina CSV: Pîvana xercê serê saetekê (%/st) û derxistina daneyan bo Excel",
            "Widgeta Ekrana Destpêkê: Nîşandana zindî ya rêjeya pîlê û hêzê li ser ekrana sereke"
        )
        AppLanguage.AR -> listOf(
            "الحماية من الحرارة الزائدة: منبه حراري قابل للضبط (38°C–52°C) بتنبيهات صوتية واهتزازية",
            "ملخص جلسة الشحن: تقرير تلقائي عند فصل الشاحن (المدة، النسبة المشحونة، القدرة، الطاقة بـ mAh)",
            "التعرف على بروتوكول الشحن: كشف فوري لـ USB-PD و PPS و Fast Charge و Qi اللاسلكي",
            "حاسبة حماية البطارية: قياس الدورات التي تم تفادي استهلاكها وتقليل الإجهاد الحراري",
            "معدل استهلاك الانتظار وتصدير CSV: قياس الاستهلاك بالساعة (%/ساعة) وتصدير البيانات لجداول Excel",
            "أداة الشاشة الرئيسية (Widget): عرض حي وسريع للنسبة والقدرة والحرارة على شاشة الهاتف"
        )
        AppLanguage.FR -> listOf(
            "Protection Thermique : Alarme de surchauffe configurable (38°C–52°C) avec sonnerie et vibration",
            "Bilan de Charge : Analyse automatique au débranchement (durée, % gagné, puissance max, énergie mAh)",
            "Détection du Protocole : Identification en temps réel de USB-PD, PPS, Fast Charge et Qi sans fil",
            "Calculateur de Préservation : Estimation des cycles d'usure évités et réduction du stress thermique",
            "Taux de Décharge & Export CSV : Mesure horaire (%/h) et export des données pour tableurs",
            "Widget Écran d'Accueil AmpereMaster : Niveau de batterie précis et puissance en direct sur l'écran d'accueil"
        )
        AppLanguage.ES -> listOf(
            "Protección Térmica: Alarma de sobrecalentamiento configurable (38°C–52°C) con audio y vibración",
            "Resumen de Carga: Balance automático al desconectar (duración, % añadido, potencia pico, energía mAh)",
            "Detección de Protocolo: Reconocimiento en vivo de USB-PD, PPS, Fast Charge y carga inalámbrica Qi",
            "Calculadora de Salud de Batería: Estimación de ciclos de desgaste evitados y menor estrés térmico",
            "Tasa de Descarga & Exportación CSV: Medición horaria (%/h) y exportación de datos para hojas de cálculo",
            "Widget de Pantalla de Inicio: Porcentaje exacto y potencia en tiempo real en la pantalla principal"
        )
        AppLanguage.RU -> listOf(
            "Защита от перегрева: Настраиваемый сигнал тревоги (38°C–52°C) со звуком и виброоткликом",
            "Итоги зарядной сессии: Автоматический отчет при отключении (длительность, +%, пиковая мощность, мАч)",
            "Распознавание протокола зарядки: Определение USB-PD, PPS, Fast Charge, стандартного USB и Qi",
            "Калькулятор сбережения аккумулятора: Оценка сохраненных циклов и снижения деградации ячеек",
            "Скорость разрядки и экспорт в CSV: Замер расхода в час (%/ч) и экспорт измерений для Excel",
            "Виджет для рабочего стола: Точный процент заряда, мощность и температура на домашнем экране"
        )
        AppLanguage.ZH -> listOf(
            "电池过热保护：可调节温度警报 (38°C–52°C)，配备声音与触觉震动预警",
            "拔出充电总结：断开线缆时自动生成报告 (充电时长、充入百分比、峰值功率、充入 mAh)",
            "快充协议智能识别：实时识别 USB-PD、PPS、快速充电、标准 USB 以及 Qi 无线充电",
            "电池健康养护计算器：直观显示避免的损耗循环与电芯低压养护收益",
            "待机放电率与 CSV 导出：每小时消耗监测 (%/小时) 及测量数据表格一键导出",
            "桌面小组件 (AppWidget)：在手机主屏幕上直接查看精准电量、功率与实时状态"
        )
        AppLanguage.HI -> listOf(
            "थर्मल सुरक्षा: ध्वनि और कंपन अलर्ट के साथ विन्यास योग्य बैटरी ओवरहीट अलार्म (38°C–52°C)",
            "चार्जिंग सत्र सारांश: केबल हटाने पर स्वचालित विश्लेषण (अवधि, जोड़ा गया %, शीर्ष शक्ति, mAh)",
            "चार्जिंग प्रोटोकॉल पहचान: USB-PD, PPS, फास्ट चार्ज, मानक USB और वायरलेस Qi की रीयल-टाइम पहचान",
            "बैटरी गार्ड कैलकुलेटर: बचाए गए बैटरी चक्रों और थर्मल तनाव में कमी का विश्लेषण",
            "डिस्चार्ज दर और CSV निर्यात: प्रति घंटे खपत का मापन (%/घंटा) और स्प्रेडशीट के लिए डेटा निर्यात",
            "होम स्क्रीन विजेट: होम स्क्रीन पर सीधे लाइव बैटरी प्रतिशत, वाट क्षमता और स्थिति देखें"
        )
    }

    val bugfixes101 = when (currentLang) {
        AppLanguage.DE -> listOf(
            "Synchronisierung des Akkustands mit der Systemanzeige des Betriebssystems korrigiert",
            "Dezimalstellen-Berechnung behoben: Kein unerwünschtes Hochlaufen nach Abstecken des Kabels",
            "Sensorfilterung optimiert beim Umschalten zwischen Lade- und Akkubetrieb"
        )
        AppLanguage.EN -> listOf(
            "Corrected battery percentage sync to match OS status bar accurately",
            "Fixed decimal calculation: stops advancing upward after unplugging cable",
            "Optimized sensor filtering when switching between charging and battery mode"
        )
        AppLanguage.TR -> listOf(
            "Pil yüzdesinin işletim sistemi durum çubuğuyla tam senkronizasyonu düzeltildi",
            "Kablo çıkarıldığında ondalık basamakların artmaya devam etmesi hatası giderildi",
            "Şarj ve pil modu geçişlerinde sensör filtreleme ve sıfırlama optimize edildi"
        )
        AppLanguage.KU -> listOf(
            "Hevdemkirina rêjeya pîlê bi nîşana pergala xebitandinê re hat rastkirin",
            "Pirsgirêka zêdebûna jimareyên dehyek piştî veqetandina kabloyê hat çareserkirin",
            "Fîlterkirina sensorê di dema guhertina rewşa barkirinê de hat başkirin"
        )
        AppLanguage.AR -> listOf(
            "تصحيح مزامنة نسبة البطارية لتتطابق بدقة مع شريط حالة نظام التشغيل",
            "إصلاح خطأ استمرار زيادة الأرقام العشرية بعد فصل كابل الشحن",
            "تحسين فلترة المستشعرات عند التبديل بين وضع الشحن وتفريغ البطارية"
        )
        AppLanguage.FR -> listOf(
            "Synchronisation exacte du pourcentage de batterie avec la barre d'état du système",
            "Correction du calcul décimal : arrêt de l'incrémentation après débranchement du câble",
            "Optimisation du filtrage des capteurs lors du basculement entre charge et batterie"
        )
        AppLanguage.ES -> listOf(
            "Sincronización exacta del porcentaje de batería con la barre de estado del sistema",
            "Corrección de decimales: ya no aumentan al desconectar el cable de carga",
            "Optimización del filtrado del sensor al cambiar entre carga y uso de batería"
        )
        AppLanguage.RU -> listOf(
            "Синхронизация уровня заряда батареи со строкой состояния системы исправлена",
            "Исправлен расчет десятичных знаков: больше не увеличиваются после отключения кабеля",
            "Оптимизирована фильтрация датчиков при переключении между зарядкой и аккумулятором"
        )
        AppLanguage.ZH -> listOf(
            "修复电量百分比与操作系统状态栏的精确同步",
            "解决拔下充电线后小数位持续异常递增的问题",
            "优化充放电状态切换时的传感器平滑滤波机制"
        )
        AppLanguage.HI -> listOf(
            "बैटरी प्रतिशत को ऑपरेटिंग सिस्टम के स्टेटस बार के साथ सटीक रूप से सिंक्रनाइज़ किया गया",
            "चार्जिंग केबल हटाने के बाद दशमलव अंकों के बढ़ते रहने की समस्या को ठीक किया गया",
            "चार्जिंग और बैटरी मोड के बीच बदलते समय सेंसर फ़िल्टरिंग को अनुकूलित किया गया"
        )
    }

    val features100 = when (currentLang) {
        AppLanguage.DE -> listOf(
            "Live-Messung von Stromstärke (mA), Spannung (V) und Ladeleistung (W)",
            "Akkustand mit 2 Nachkommastellen und dynamischer Restladezeit-Berechnung",
            "Interaktiver Ladekurven-Graph mit Canvas-Rendering und Scrubber-Fadenkreuz",
            "10 Sprachen mit RTL-Unterstützung sowie True Black OLED-Modus (#000000)"
        )
        AppLanguage.EN -> listOf(
            "Live measurement of current (mA), voltage (V), and charging power (W)",
            "Battery percentage with 2 decimal places and dynamic remaining time estimation",
            "Interactive charging curve canvas graph with touch scrubber crosshair",
            "10 languages with native RTL support and pure True Black OLED theme (#000000)"
        )
        AppLanguage.TR -> listOf(
            "Akım (mA), voltaj (V) ve şarj gücünün (W) gerçek zamanlı canlı ölçümü",
            "2 ondalık basamaklı pil yüzdesi ve dinamik kalan şarj süresi tahmini",
            "Dokunmatik imleçli interaktif şarj eğrisi Canvas grafiği",
            "RTL destekli 10 dil seçeneği ve saf True Black OLED teması (#000000)"
        )
        AppLanguage.KU -> listOf(
            "Pîvana zindî ya herikînê (mA), voltaj (V) û hêza barkirinê (W)",
            "Rêjeya pîlê bi 2 dehyekan û hejmartina dema mayî ya barkirinê",
            "Xêza înteraktîf a barkirinê bi Canvas û tiliya şopandinê",
            "10 ziman bi piştgiriya RTL û moda True Black ya OLED (#000000)"
        )
        AppLanguage.AR -> listOf(
            "قياس مباشر للتيار (mA) والجهد (V) وقدرة الشحن (W)",
            "نسبة بطارية بدقة رقمين عشريين وحساب الوقت المتبقي للشحن",
            "رسم بياني تفاعلي لمنحنى الشحن بمؤشر تتبع لمسي",
            "10 لغات مع دعم كامل للاتجاه من اليمين لليسار ونمط True Black لشاشات OLED"
        )
        AppLanguage.FR -> listOf(
            "Mesure en direct du courant (mA), de la tension (V) et de la puissance (W)",
            "Niveau de batterie à 2 décimales et calcul dynamique du temps restant",
            "Graphique interactif sur Canvas avec curseur tactile de précision",
            "10 langues avec support RTL et thème True Black pur pour OLED (#000000)"
        )
        AppLanguage.ES -> listOf(
            "Medición en vivo de corriente (mA), voltaje (V) y potencia de carga (W)",
            "Nivel de batería con 2 decimales y cálculo dinámico de tiempo restante",
            "Gráfica interactiva de curva de carga con visor táctil de precisión",
            "10 idiomas con soporte RTL y modo True Black puro para OLED (#000000)"
        )
        AppLanguage.RU -> listOf(
            "Измерение силы тока (мА), напряжения (В) и мощности зарядки (Вт) в реальном времени",
            "Уровень заряда с 2 знаками после запятой и динамический расчет оставшегося времени",
            "Интерактивный холст графика зарядки с сенсорным визиром",
            "10 языков с поддержкой RTL и чистый режим True Black для OLED (#000000)"
        )
        AppLanguage.ZH -> listOf(
            "实时测量电流 (mA)、电压 (V) 及充电功率 (W)",
            "高精度 2 位小数电量百分比及充满剩余时间动态预估",
            "支持触控交互十字准星指示的 Canvas 充电曲线图表",
            "支持 RTL 布局的 10 种语言及专为 OLED 打造的纯黑模式 (#000000)"
        )
        AppLanguage.HI -> listOf(
            "करंट (mA), वोल्टेज (V) और चार्जिंग पावर (W) का रीयल-टाइम मापन",
            "2 दशमलव स्थानों के साथ सटीक बैटरी प्रतिशत और शेष समय गणना",
            "टच स्क्रबर के साथ इंटरैक्टिव चार्जिंग वक्र कैनवास ग्राफ",
            "RTL समर्थन के साथ 10 भाषाएं और शुद्ध True Black OLED मोड (#000000)"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ChargingCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = ChargingCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = strings.changelogTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Version 1.0.4 (Aktuell)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "v1.0.4 (Pro Suite & Adaptive UI)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = ElectricGreen
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ElectricGreen.copy(alpha = 0.18f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = currentBadge,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = ElectricGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        features104.forEach { item ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "• ",
                                    color = ElectricGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Version 1.0.3
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "v1.0.3 (Analytics & Benchmark)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = ChargingCyan
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ChargingCyan.copy(alpha = 0.18f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = prevBadge,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = ChargingCyan
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        features103.forEach { item ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "• ",
                                    color = ChargingCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Version 1.0.2
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "v1.0.2 (Feature Update)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        features102.forEach { item ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "• ",
                                    color = MaterialTheme.colorScheme.outline,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Version 1.0.1
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "v1.0.1 (Bugfix Release)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = ChargingCyan
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ChargingCyan.copy(alpha = 0.18f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = prevBadge,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = ChargingCyan
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        bugfixes101.forEach { item ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "• ",
                                    color = ChargingCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Version 1.0.0
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "v1.0.0 (Initial Release)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        features100.forEach { item ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "• ",
                                    color = MaterialTheme.colorScheme.outline,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss
            ) {
                Text(strings.close)
            }
        }
    )
}

import { useState } from 'react';
import { Modal, StyleSheet, Text, TouchableOpacity, View } from 'react-native';

// ─── Tipos ────────────────────────────────────────────────────────────────────
type Props = {
  startDate: string;   // 'YYYY-MM-DD' o ''
  endDate: string;     // 'YYYY-MM-DD' o ''
  onChange: (start: string, end: string) => void;
};

// ─── Helpers ──────────────────────────────────────────────────────────────────
const MESES = ['Enero','Febrero','Marzo','Abril','Mayo','Junio','Julio','Agosto','Septiembre','Octubre','Noviembre','Diciembre'];
const DIAS_SEMANA = ['L','M','X','J','V','S','D'];

function toYMD(date: Date): string {
  return date.toISOString().split('T')[0];
}

function fromYMD(ymd: string): Date {
  const [y, m, d] = ymd.split('-').map(Number);
  return new Date(y, m - 1, d);
}

function daysInMonth(year: number, month: number): number {
  return new Date(year, month + 1, 0).getDate();
}

function firstDayOfMonth(year: number, month: number): number {
  // 0=Dom→6, queremos 0=Lun
  const d = new Date(year, month, 1).getDay();
  return (d + 6) % 7;
}

function formatDisplay(ymd: string): string {
  if (!ymd) return '';
  const d = fromYMD(ymd);
  return d.toLocaleDateString('es-ES', { day: 'numeric', month: 'short', year: 'numeric' });
}

// ─── Componente ───────────────────────────────────────────────────────────────
export default function DateRangePicker({ startDate, endDate, onChange }: Props) {
  const today = new Date();
  const [visible, setVisible] = useState(false);
  const [viewYear, setViewYear] = useState(today.getFullYear());
  const [viewMonth, setViewMonth] = useState(today.getMonth());
  // Durante la selección: primera fecha ya tocada
  const [selecting, setSelecting] = useState<string | null>(null);

  function prevMonth() {
    if (viewMonth === 0) { setViewMonth(11); setViewYear(y => y - 1); }
    else setViewMonth(m => m - 1);
  }

  function nextMonth() {
    if (viewMonth === 11) { setViewMonth(0); setViewYear(y => y + 1); }
    else setViewMonth(m => m + 1);
  }

  function handleDayPress(ymd: string) {
    if (!selecting) {
      // Primera pulsación = fecha inicio
      setSelecting(ymd);
      onChange(ymd, '');
    } else {
      // Segunda pulsación = fecha fin
      if (ymd < selecting) {
        // Si toca antes del inicio, reinicia
        setSelecting(ymd);
        onChange(ymd, '');
      } else {
        onChange(selecting, ymd);
        setSelecting(null);
        setVisible(false);
      }
    }
  }

  function getDayStyle(ymd: string) {
    const isStart = ymd === startDate;
    const isEnd = ymd === endDate;
    const inRange = startDate && endDate && ymd > startDate && ymd < endDate;
    const isToday = ymd === toYMD(today);
    const isPast = ymd < toYMD(today);

    return {
      isStart, isEnd, inRange: !!inRange, isToday, isPast,
    };
  }

  // Genera las celdas del mes
  function buildCells() {
    const totalDays = daysInMonth(viewYear, viewMonth);
    const firstDay = firstDayOfMonth(viewYear, viewMonth);
    const cells: Array<{ ymd: string | null }> = [];

    // Espacios vacíos al inicio
    for (let i = 0; i < firstDay; i++) cells.push({ ymd: null });

    for (let d = 1; d <= totalDays; d++) {
      const ymd = `${viewYear}-${String(viewMonth + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
      cells.push({ ymd });
    }
    return cells;
  }

  const cells = buildCells();

  // Texto del botón
  const btnText = startDate && endDate
    ? `${formatDisplay(startDate)} → ${formatDisplay(endDate)}`
    : startDate
    ? `${formatDisplay(startDate)} → elige fin`
    : 'Selecciona las fechas';

  return (
    <>
      {/* Botón que abre el modal */}
      <TouchableOpacity style={styles.trigger} onPress={() => { setSelecting(null); setVisible(true); }}>
        <Text style={styles.triggerIcon}>📅</Text>
        <Text style={[styles.triggerText, !(startDate) && styles.triggerPlaceholder]}>
          {btnText}
        </Text>
        {(startDate || endDate) && (
          <TouchableOpacity onPress={() => { onChange('', ''); setSelecting(null); }}>
            <Text style={styles.triggerClear}>✕</Text>
          </TouchableOpacity>
        )}
      </TouchableOpacity>

      {/* Modal calendario */}
      <Modal visible={visible} transparent animationType="slide" onRequestClose={() => setVisible(false)}>
        <View style={styles.overlay}>
          <View style={styles.sheet}>
            {/* Handle */}
            <View style={styles.handle} />

            {/* Instrucción */}
            <Text style={styles.instruction}>
              {!selecting ? 'Toca el día de inicio' : 'Toca el día de fin'}
            </Text>

            {/* Navegación mes */}
            <View style={styles.navRow}>
              <TouchableOpacity style={styles.navBtn} onPress={prevMonth}>
                <Text style={styles.navArrow}>‹</Text>
              </TouchableOpacity>
              <Text style={styles.navTitle}>{MESES[viewMonth]} {viewYear}</Text>
              <TouchableOpacity style={styles.navBtn} onPress={nextMonth}>
                <Text style={styles.navArrow}>›</Text>
              </TouchableOpacity>
            </View>

            {/* Cabecera días de semana */}
            <View style={styles.weekHeader}>
              {DIAS_SEMANA.map(d => (
                <Text key={d} style={styles.weekDay}>{d}</Text>
              ))}
            </View>

            {/* Grid días */}
            <View style={styles.grid}>
              {cells.map((cell, i) => {
                if (!cell.ymd) return <View key={`empty-${i}`} style={styles.cell} />;

                const { isStart, isEnd, inRange, isToday, isPast } = getDayStyle(cell.ymd);
                const day = parseInt(cell.ymd.split('-')[2]);

                return (
                  <TouchableOpacity
                    key={cell.ymd}
                    style={[
                      styles.cell,
                      inRange && styles.cellInRange,
                      (isStart || isEnd) && styles.cellSelected,
                      isStart && styles.cellStart,
                      isEnd && styles.cellEnd,
                    ]}
                    onPress={() => !isPast && handleDayPress(cell.ymd!)}
                    disabled={isPast}
                  >
                    <Text style={[
                      styles.cellText,
                      isPast && styles.cellTextPast,
                      isToday && styles.cellTextToday,
                      (isStart || isEnd) && styles.cellTextSelected,
                    ]}>
                      {day}
                    </Text>
                  </TouchableOpacity>
                );
              })}
            </View>

            {/* Resumen selección */}
            {(startDate || endDate) && (
              <View style={styles.summary}>
                <View style={styles.summaryItem}>
                  <Text style={styles.summaryLabel}>Inicio</Text>
                  <Text style={styles.summaryValue}>{formatDisplay(startDate) || '—'}</Text>
                </View>
                <Text style={styles.summaryArrow}>→</Text>
                <View style={styles.summaryItem}>
                  <Text style={styles.summaryLabel}>Fin</Text>
                  <Text style={styles.summaryValue}>{formatDisplay(endDate) || '—'}</Text>
                </View>
                {startDate && endDate && (
                  <View style={styles.summaryDuration}>
                    <Text style={styles.summaryDurationText}>
                      {Math.round((fromYMD(endDate).getTime() - fromYMD(startDate).getTime()) / (1000 * 60 * 60 * 24))} días
                    </Text>
                  </View>
                )}
              </View>
            )}

            {/* Botón cancelar */}
            <TouchableOpacity style={styles.cancelBtn} onPress={() => { setSelecting(null); setVisible(false); }}>
              <Text style={styles.cancelText}>Cancelar</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
    </>
  );
}

const CELL_SIZE = 44;

const styles = StyleSheet.create({
  // Trigger
  trigger: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#F8F6F2', borderRadius: 10, padding: 12, borderWidth: 0.5, borderColor: '#EAE6E0', gap: 8 },
  triggerIcon: { fontSize: 16 },
  triggerText: { flex: 1, fontSize: 14, color: '#1a1a1a' },
  triggerPlaceholder: { color: '#ccc' },
  triggerClear: { fontSize: 13, color: '#ccc', paddingHorizontal: 4 },

  // Modal
  overlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'flex-end' },
  sheet: { backgroundColor: '#fff', borderTopLeftRadius: 24, borderTopRightRadius: 24, paddingBottom: 34, paddingHorizontal: 16, paddingTop: 12 },
  handle: { width: 36, height: 4, backgroundColor: '#E0DDD8', borderRadius: 2, alignSelf: 'center', marginBottom: 16 },

  instruction: { fontSize: 13, color: '#D95F2B', fontWeight: '700', textAlign: 'center', marginBottom: 16 },

  navRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 },
  navBtn: { width: 36, height: 36, alignItems: 'center', justifyContent: 'center' },
  navArrow: { fontSize: 24, color: '#1a1a1a' },
  navTitle: { fontSize: 16, fontWeight: '800', color: '#1a1a1a' },

  weekHeader: { flexDirection: 'row', marginBottom: 4 },
  weekDay: { width: CELL_SIZE, textAlign: 'center', fontSize: 11, color: '#bbb', fontWeight: '600' },

  grid: { flexDirection: 'row', flexWrap: 'wrap' },
  cell: { width: CELL_SIZE, height: CELL_SIZE, alignItems: 'center', justifyContent: 'center' },
  cellInRange: { backgroundColor: '#FFF4EF' },
  cellSelected: { backgroundColor: '#D95F2B', borderRadius: CELL_SIZE / 2 },
  cellStart: { borderTopLeftRadius: CELL_SIZE / 2, borderBottomLeftRadius: CELL_SIZE / 2 },
  cellEnd: { borderTopRightRadius: CELL_SIZE / 2, borderBottomRightRadius: CELL_SIZE / 2 },
  cellText: { fontSize: 14, color: '#1a1a1a' },
  cellTextPast: { color: '#ddd' },
  cellTextToday: { fontWeight: '800', color: '#D95F2B' },
  cellTextSelected: { color: '#fff', fontWeight: '700' },

  summary: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#F8F6F2', borderRadius: 12, padding: 12, marginTop: 12, gap: 8 },
  summaryItem: { flex: 1, alignItems: 'center' },
  summaryLabel: { fontSize: 10, color: '#aaa', marginBottom: 2 },
  summaryValue: { fontSize: 13, fontWeight: '700', color: '#1a1a1a' },
  summaryArrow: { fontSize: 16, color: '#D95F2B' },
  summaryDuration: { backgroundColor: '#D95F2B', borderRadius: 10, paddingHorizontal: 8, paddingVertical: 3 },
  summaryDurationText: { fontSize: 11, color: '#fff', fontWeight: '700' },

  cancelBtn: { marginTop: 12, paddingVertical: 14, alignItems: 'center' },
  cancelText: { fontSize: 15, color: '#aaa' },
});

import * as ImagePicker from 'expo-image-picker';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import {
  Alert,
  Image,
  SafeAreaView, ScrollView, StyleSheet, Text,
  TextInput, TouchableOpacity, View
} from 'react-native';
import DateRangePicker from '../components/DateRangePicker';
import { supabase } from '../services/supabase';

const TRIP_TYPES = ['Urbano', 'Cultural', 'Naturaleza', 'Gastronómico', 'Fiesta', 'Playa', 'Aventura', 'Relax', 'Road trip', 'Mochilero'];
const COST_CATEGORIES = ['Transporte ida/vuelta', 'Transporte local', 'Alojamiento', 'Comida y bebida', 'Actividades', 'Souvenirs', 'Otro'];

type DayEntry = { date: string; morning: string; afternoon: string };
type CostItem = { category: string; amount: string };

export default function PostScreen() {
  const router = useRouter();

  const [title, setTitle] = useState('');
  const [destination, setDestination] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [cost, setCost] = useState('');
  const [tripTypes, setTripTypes] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);

  const [openSections, setOpenSections] = useState<Record<string, boolean>>({
    itinerary: false,
    costs: false,
    companions: false,
  });

  const [days, setDays] = useState<DayEntry[]>([]);
  const [costItems, setCostItems] = useState<CostItem[]>(
    COST_CATEGORIES.map(cat => ({ category: cat, amount: '' }))
  );

  const [coverUri, setCoverUri] = useState<string | null>(null);
  const [companionSearch, setCompanionSearch] = useState('');
  const [companionResults, setCompanionResults] = useState<any[]>([]);
  const [selectedCompanions, setSelectedCompanions] = useState<any[]>([]);

  // ─── Helpers ────────────────────────────────────────────────────────────────

  function toggleType(type: string) {
    setTripTypes(prev =>
      prev.includes(type) ? prev.filter(t => t !== type) : [...prev, type]
    );
  }

  function toggleSection(key: string) {
    const opening = !openSections[key];
    setOpenSections(prev => ({ ...prev, [key]: opening }));
    if (key === 'itinerary' && opening) generateDays();
  }

  function generateDays() {
    if (!startDate || !endDate) return;
    const start = new Date(startDate);
    const end = new Date(endDate);
    if (isNaN(start.getTime()) || isNaN(end.getTime())) return;

    const generated: DayEntry[] = [];
    const current = new Date(start);
    while (current <= end) {
      const dateStr = current.toISOString().split('T')[0];
      const existing = days.find(d => d.date === dateStr);
      generated.push(existing ?? { date: dateStr, morning: '', afternoon: '' });
      current.setDate(current.getDate() + 1);
    }
    setDays(generated);
  }

  function updateDay(index: number, field: 'morning' | 'afternoon', value: string) {
    setDays(prev => prev.map((d, i) => i === index ? { ...d, [field]: value } : d));
  }

  const totalCostsEntered = costItems.reduce((sum, item) => sum + (parseFloat(item.amount) || 0), 0);
  const totalBudget = parseFloat(cost) || 0;
  const remaining = totalBudget - totalCostsEntered;

  function updateCostItem(index: number, value: string) {
    setCostItems(prev => prev.map((item, i) => i === index ? { ...item, amount: value } : item));
  }

  async function searchCompanions(text: string) {
    setCompanionSearch(text);
    if (text.length < 2) { setCompanionResults([]); return; }
    const { data: { user: me } } = await supabase.auth.getUser();
    const { data } = await supabase
      .from('users')
      .select('id, username')
      .ilike('username', `%${text}%`)
      .neq('id', me?.id ?? '')
      .limit(5);
    setCompanionResults(data || []);
  }

  function addCompanion(user: any) {
    if (selectedCompanions.find(c => c.id === user.id)) return;
    setSelectedCompanions(prev => [...prev, user]);
    setCompanionSearch('');
    setCompanionResults([]);
  }

  function removeCompanion(id: string) {
    setSelectedCompanions(prev => prev.filter(c => c.id !== id));
  }

  // ─── Foto de portada ────────────────────────────────────────────────────────

  async function pickCoverPhoto() {
    const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (status !== 'granted') {
      Alert.alert('Permiso denegado', 'Necesitamos acceso a tu galería para subir la foto');
      return;
    }
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsEditing: true,
      aspect: [16, 9],
      quality: 0.8,
    });
    if (!result.canceled && result.assets[0]) {
      setCoverUri(result.assets[0].uri);
    }
  }

  async function uploadCoverPhoto(userId: string, tripId: string): Promise<string | null> {
    if (!coverUri) return null;
    Alert.alert('DEBUG', `Intentando subir: ${coverUri}`);
    try {
      // Convertir URI a ArrayBuffer (compatible con Expo/React Native)
      const response = await fetch(coverUri);
      const arrayBuffer = await response.arrayBuffer();
      const uint8Array = new Uint8Array(arrayBuffer);

      const ext = coverUri.split('.').pop()?.toLowerCase() || 'jpg';
      const path = `${userId}/${tripId}.${ext}`;
      const contentType = ext === 'png' ? 'image/png' : 'image/jpeg';

      const { error } = await supabase.storage
        .from('trip-covers')
        .upload(path, uint8Array, {
          contentType,
          upsert: true,
        });

      if (error) {
        Alert.alert('Error storage', error.message); // ← añade esto
        console.error('Error subiendo foto:', error.message);
        return null;
      }

      const { data } = supabase.storage.from('trip-covers').getPublicUrl(path);
      return data.publicUrl;
    } catch (e: any) {
      console.error('Error en uploadCoverPhoto:', JSON.stringify(e));
      Alert.alert('Error foto', e.message || JSON.stringify(e));
      return null;
    }
  }

  // ─── Publicar ───────────────────────────────────────────────────────────────

  async function handlePublish() {
    Alert.alert('DEBUG', `coverUri = ${coverUri ?? 'NULL'}`); // ← primera línea
    if (!title || !destination || !startDate || !endDate || !cost) {
      Alert.alert('Faltan datos', 'Rellena todos los campos obligatorios');
      return;
    }

    setLoading(true);
    try {
      const { data: { user } } = await supabase.auth.getUser();
      if (!user) throw new Error('No hay sesión activa');

      // 1. Insertar viaje (sin foto aún)
      const { data: trip, error: tripError } = await supabase
        .from('trips')
        .insert({
          user_id: user.id,
          title,
          destination,
          start_date: startDate,
          end_date: endDate,
          total_cost: parseInt(cost),
          currency: 'EUR',
          trip_type: tripTypes,
          is_published: true,
        })
        .select()
        .single();

      if (tripError || !trip) throw new Error(tripError?.message || 'Error creando viaje');

      // 2. Subir foto y actualizar la URL (si hay foto)
      if (coverUri) {
        Alert.alert('DEBUG', 'coverUri existe, llamando upload...');
        const coverUrl = await uploadCoverPhoto(user.id, trip.id);
        if (coverUrl) {
          await supabase
            .from('trips')
            .update({ cover_photo_url: coverUrl })
            .eq('id', trip.id);
        }
      }

      // 3. Guardar días del itinerario
      if (openSections.itinerary && days.length > 0) {
        const daysToInsert = days
          .filter(d => d.morning.trim() || d.afternoon.trim())
          .map((d, i) => ({
            trip_id: trip.id,
            day_number: i + 1,
            day_date: d.date,
            morning_description: d.morning.trim() || null,
            afternoon_description: d.afternoon.trim() || null,
          }));
        if (daysToInsert.length > 0) {
          await supabase.from('trip_days').insert(daysToInsert);
        }
      }

      // 4. Guardar costes
      if (openSections.costs) {
        const costsToInsert = costItems
          .filter(item => parseFloat(item.amount) > 0)
          .map(item => ({
            trip_id: trip.id,
            category: item.category,
            amount: parseFloat(item.amount),
            currency: 'EUR',
          }));
        if (costsToInsert.length > 0) {
          await supabase.from('cost_items').insert(costsToInsert);
        }
      }

      // 5. Guardar compañeros
      if (openSections.companions && selectedCompanions.length > 0) {
        await supabase.from('companions').insert(
          selectedCompanions.map(c => ({ trip_id: trip.id, user_id: c.id }))
        );
      }

      // ✅ Navegar directamente al feed sin esperar que el usuario toque nada
      router.replace('/(tabs)/' as any);

    } catch (e: any) {
      Alert.alert('Error al publicar', e.message || 'Inténtalo de nuevo');
    } finally {
      setLoading(false);
    }
  }

  // ─── Render ─────────────────────────────────────────────────────────────────

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()}>
          <Text style={styles.cancel}>✕</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Nuevo viaje</Text>
        <TouchableOpacity
          style={[styles.publishBtn, loading && styles.publishBtnDisabled]}
          onPress={handlePublish}
          disabled={loading}
        >
          <Text style={styles.publishText}>{loading ? 'Publicando...' : 'Publicar'}</Text>
        </TouchableOpacity>
      </View>

      <ScrollView contentContainerStyle={styles.form}>

        {/* ── FOTO DE PORTADA ──────────────────────────────────────────────── */}
        <TouchableOpacity style={styles.uploadZone} onPress={pickCoverPhoto} activeOpacity={0.7}>
          {coverUri ? (
            <Image source={{ uri: coverUri }} style={styles.coverPreview} resizeMode="cover" />
          ) : (
            <>
              <Text style={styles.uploadIcon}>📷</Text>
              <Text style={styles.uploadText}>Toca para añadir foto de portada</Text>
            </>
          )}
        </TouchableOpacity>

        {/* ── CAMPOS BÁSICOS ───────────────────────────────────────────────── */}
        {[
          { label: '📍 Destino', value: destination, onChange: setDestination, placeholder: 'ej. Tokyo, Japón' },
        ].map((field, i) => (
          <View key={i} style={styles.field}>
            <Text style={styles.fieldLabel}>{field.label}</Text>
            <TextInput
              style={styles.fieldInput}
              value={field.value}
              onChangeText={field.onChange}
              placeholder={field.placeholder}
              placeholderTextColor="#ccc"
            />
          </View>
        ))}

        {/* ── FECHAS CON CALENDARIO ────────────────────────────────────────── */}
        <View style={styles.field}>
          <Text style={styles.fieldLabel}>📅 Fechas del viaje</Text>
          <DateRangePicker
            startDate={startDate}
            endDate={endDate}
            onChange={(start, end) => { setStartDate(start); setEndDate(end); }}
          />
        </View>

        {/* ── COSTE ────────────────────────────────────────────────────────── */}
        {[
          { label: '💰 Coste total (€)', value: cost, onChange: setCost, placeholder: 'ej. 1240', keyboard: 'numeric' },
        ].map((field, i) => (
          <View key={i} style={styles.field}>
            <Text style={styles.fieldLabel}>{field.label}</Text>
            <TextInput
              style={styles.fieldInput}
              value={field.value}
              onChangeText={field.onChange}
              placeholder={field.placeholder}
              placeholderTextColor="#ccc"
              keyboardType={(field as any).keyboard || 'default'}
            />
          </View>
        ))}

        <View style={styles.field}>
          <Text style={styles.fieldLabel}>✏️ Título del viaje</Text>
          <TextInput
            style={[styles.fieldInput, styles.fieldInputTall]}
            value={title}
            onChangeText={setTitle}
            placeholder="ej. 10 días en Tokio sin gastar una fortuna"
            placeholderTextColor="#ccc"
            multiline
          />
        </View>

        {/* ── TIPO DE VIAJE ────────────────────────────────────────────────── */}
        <View style={styles.field}>
          <Text style={styles.fieldLabel}>🧭 Tipo de viaje (opcional)</Text>
          <View style={styles.chips}>
            {TRIP_TYPES.map(type => {
              const selected = tripTypes.includes(type);
              return (
                <TouchableOpacity
                  key={type}
                  style={[styles.chip, selected && styles.chipSelected]}
                  onPress={() => toggleType(type)}
                >
                  <Text style={[styles.chipText, selected && styles.chipTextSelected]}>
                    {selected ? '✓ ' : ''}{type}
                  </Text>
                </TouchableOpacity>
              );
            })}
          </View>
        </View>

        <Text style={styles.sectionsDivider}>SECCIONES OPCIONALES</Text>

        {/* ── ITINERARIO ───────────────────────────────────────────────────── */}
        <TouchableOpacity
          style={[styles.sectionToggle, openSections.itinerary && styles.sectionToggleOpen]}
          onPress={() => toggleSection('itinerary')}
        >
          <Text style={styles.sectionToggleText}>📅 Itinerario por días</Text>
          <Text style={styles.sectionToggleArrow}>{openSections.itinerary ? '▲' : '▼'}</Text>
        </TouchableOpacity>

        {openSections.itinerary && (
          <View style={styles.sectionBody}>
            {days.length === 0 ? (
              <Text style={styles.sectionHint}>
                Introduce las fechas de inicio y fin para generar el itinerario automáticamente.
              </Text>
            ) : (
              days.map((day, i) => (
                <View key={day.date} style={styles.dayCard}>
                  <Text style={styles.dayTitle}>
                    Día {i + 1} · {new Date(day.date).toLocaleDateString('es-ES', { weekday: 'short', day: 'numeric', month: 'short' })}
                  </Text>
                  <TextInput
                    style={styles.dayInput}
                    value={day.morning}
                    onChangeText={v => updateDay(i, 'morning', v)}
                    placeholder="Mañana — ¿qué hiciste?"
                    placeholderTextColor="#ccc"
                    multiline
                  />
                  <TextInput
                    style={styles.dayInput}
                    value={day.afternoon}
                    onChangeText={v => updateDay(i, 'afternoon', v)}
                    placeholder="Tarde — ¿qué hiciste?"
                    placeholderTextColor="#ccc"
                    multiline
                  />
                </View>
              ))
            )}
          </View>
        )}

        {/* ── DESGLOSE DE COSTES ───────────────────────────────────────────── */}
        <TouchableOpacity
          style={[styles.sectionToggle, openSections.costs && styles.sectionToggleOpen]}
          onPress={() => toggleSection('costs')}
        >
          <Text style={styles.sectionToggleText}>💸 Desglose de costes</Text>
          <Text style={styles.sectionToggleArrow}>{openSections.costs ? '▲' : '▼'}</Text>
        </TouchableOpacity>

        {openSections.costs && (
          <View style={styles.sectionBody}>
            <View style={styles.budgetBar}>
              <Text style={styles.budgetLabel}>
                Asignado: <Text style={styles.budgetAmount}>€{totalCostsEntered.toFixed(0)}</Text>
                {' / '}Total: <Text style={styles.budgetAmount}>€{totalBudget.toFixed(0)}</Text>
              </Text>
              <Text style={[styles.budgetRemaining, remaining < 0 && styles.budgetOver]}>
                {remaining >= 0 ? `Resta €${remaining.toFixed(0)}` : `Excede €${Math.abs(remaining).toFixed(0)}`}
              </Text>
            </View>
            {costItems.map((item, i) => (
              <View key={item.category} style={styles.costRow}>
                <Text style={styles.costCategory}>{item.category}</Text>
                <View style={styles.costInputWrap}>
                  <Text style={styles.costEuro}>€</Text>
                  <TextInput
                    style={styles.costInput}
                    value={item.amount}
                    onChangeText={v => updateCostItem(i, v)}
                    placeholder="0"
                    placeholderTextColor="#ccc"
                    keyboardType="numeric"
                  />
                </View>
              </View>
            ))}
            {remaining > 0 && (
              <View style={styles.autoOther}>
                <Text style={styles.autoOtherText}>
                  💡 €{remaining.toFixed(0)} se asignarán automáticamente a "Otro"
                </Text>
              </View>
            )}
          </View>
        )}

        {/* ── COMPAÑEROS ───────────────────────────────────────────────────── */}
        <TouchableOpacity
          style={[styles.sectionToggle, openSections.companions && styles.sectionToggleOpen]}
          onPress={() => toggleSection('companions')}
        >
          <Text style={styles.sectionToggleText}>👥 Compañeros de viaje</Text>
          <Text style={styles.sectionToggleArrow}>{openSections.companions ? '▲' : '▼'}</Text>
        </TouchableOpacity>

        {openSections.companions && (
          <View style={styles.sectionBody}>
            {selectedCompanions.length > 0 && (
              <View style={styles.selectedCompanions}>
                {selectedCompanions.map(c => (
                  <View key={c.id} style={styles.companionChip}>
                    <Text style={styles.companionChipText}>@{c.username}</Text>
                    <TouchableOpacity onPress={() => removeCompanion(c.id)}>
                      <Text style={styles.companionRemove}> ✕</Text>
                    </TouchableOpacity>
                  </View>
                ))}
              </View>
            )}
            <TextInput
              style={styles.fieldInput}
              value={companionSearch}
              onChangeText={searchCompanions}
              placeholder="Buscar por username..."
              placeholderTextColor="#ccc"
              autoCapitalize="none"
            />
            {companionResults.map(user => (
              <TouchableOpacity
                key={user.id}
                style={styles.companionResult}
                onPress={() => addCompanion(user)}
              >
                <View style={styles.companionAvatar}>
                  <Text style={styles.companionAvatarText}>
                    {user.username.slice(0, 2).toUpperCase()}
                  </Text>
                </View>
                <Text style={styles.companionUsername}>@{user.username}</Text>
                <Text style={styles.companionAdd}>+ Añadir</Text>
              </TouchableOpacity>
            ))}
          </View>
        )}

        {/* ── PRÓXIMAMENTE ─────────────────────────────────────────────────── */}
        {[
          { icon: '🗺️', label: 'Mapa IA' },
          { icon: '📸', label: 'Fotos' },
        ].map(s => (
          <View key={s.label} style={styles.sectionToggleSoon}>
            <Text style={styles.sectionToggleText}>{s.icon} {s.label}</Text>
            <Text style={styles.soonBadge}>Próximamente</Text>
          </View>
        ))}

        <View style={{ height: 40 }} />
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 16, paddingVertical: 14, borderBottomWidth: 0.5, borderBottomColor: '#F0EDE8' },
  cancel: { fontSize: 18, color: '#ccc', width: 32 },
  headerTitle: { fontSize: 16, fontWeight: '800', color: '#1a1a1a' },
  publishBtn: { backgroundColor: '#D95F2B', borderRadius: 20, paddingHorizontal: 16, paddingVertical: 6 },
  publishBtnDisabled: { backgroundColor: '#ccc' },
  publishText: { color: '#fff', fontSize: 13, fontWeight: '600' },
  form: { padding: 16, gap: 14 },

  uploadZone: { borderWidth: 1.5, borderColor: '#E0DDD8', borderStyle: 'dashed', borderRadius: 14, height: 180, alignItems: 'center', justifyContent: 'center', gap: 8, overflow: 'hidden' },
  uploadIcon: { fontSize: 28 },
  uploadText: { fontSize: 13, color: '#bbb' },
  coverPreview: { width: '100%', height: '100%' },

  field: { gap: 6 },
  fieldLabel: { fontSize: 12, color: '#888', fontWeight: '500' },
  fieldInput: { backgroundColor: '#F8F6F2', borderRadius: 10, padding: 12, fontSize: 14, color: '#1a1a1a', borderWidth: 0.5, borderColor: '#EAE6E0' },
  fieldInputTall: { height: 80, textAlignVertical: 'top' },

  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  chip: { backgroundColor: '#F5F2ED', borderWidth: 0.5, borderColor: '#E0DDD8', borderRadius: 20, paddingHorizontal: 12, paddingVertical: 5 },
  chipSelected: { backgroundColor: '#1a1a1a', borderColor: '#1a1a1a' },
  chipText: { fontSize: 11, color: '#888' },
  chipTextSelected: { color: '#fff' },

  sectionsDivider: { fontSize: 10, color: '#ccc', textTransform: 'uppercase', letterSpacing: 1, textAlign: 'center', marginTop: 8 },
  sectionToggle: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', backgroundColor: '#F8F6F2', borderRadius: 12, padding: 14, borderWidth: 0.5, borderColor: '#EAE6E0' },
  sectionToggleOpen: { backgroundColor: '#FFF4EF', borderColor: '#D95F2B' },
  sectionToggleText: { fontSize: 14, fontWeight: '600', color: '#1a1a1a' },
  sectionToggleArrow: { fontSize: 10, color: '#aaa' },
  sectionToggleSoon: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', backgroundColor: '#F8F6F2', borderRadius: 12, padding: 14, borderWidth: 0.5, borderColor: '#EAE6E0', opacity: 0.6 },
  soonBadge: { fontSize: 10, color: '#D95F2B', backgroundColor: '#FFF4EF', paddingHorizontal: 8, paddingVertical: 2, borderRadius: 10, fontWeight: '600', overflow: 'hidden' },
  sectionBody: { backgroundColor: '#F8F6F2', borderRadius: 12, padding: 14, gap: 10, borderWidth: 0.5, borderColor: '#EAE6E0', marginTop: -8 },
  sectionHint: { fontSize: 12, color: '#aaa', textAlign: 'center', paddingVertical: 8 },

  dayCard: { backgroundColor: '#fff', borderRadius: 10, padding: 12, gap: 8, borderWidth: 0.5, borderColor: '#EAE6E0' },
  dayTitle: { fontSize: 12, fontWeight: '700', color: '#D95F2B' },
  dayInput: { backgroundColor: '#F8F6F2', borderRadius: 8, padding: 10, fontSize: 13, color: '#1a1a1a', minHeight: 56, textAlignVertical: 'top', borderWidth: 0.5, borderColor: '#EAE6E0' },

  budgetBar: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', backgroundColor: '#fff', borderRadius: 10, padding: 10, borderWidth: 0.5, borderColor: '#EAE6E0' },
  budgetLabel: { fontSize: 12, color: '#888' },
  budgetAmount: { fontWeight: '700', color: '#1a1a1a' },
  budgetRemaining: { fontSize: 12, fontWeight: '700', color: '#4CAF50' },
  budgetOver: { color: '#E0443A' },
  costRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', backgroundColor: '#fff', borderRadius: 10, paddingHorizontal: 12, paddingVertical: 10, borderWidth: 0.5, borderColor: '#EAE6E0' },
  costCategory: { fontSize: 13, color: '#1a1a1a', flex: 1 },
  costInputWrap: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  costEuro: { fontSize: 13, color: '#aaa' },
  costInput: { width: 70, textAlign: 'right', fontSize: 14, fontWeight: '600', color: '#1a1a1a', padding: 4 },
  autoOther: { backgroundColor: '#FFF4EF', borderRadius: 10, padding: 10 },
  autoOtherText: { fontSize: 12, color: '#D95F2B' },

  selectedCompanions: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  companionChip: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#1a1a1a', borderRadius: 20, paddingHorizontal: 10, paddingVertical: 5 },
  companionChipText: { fontSize: 12, color: '#fff' },
  companionRemove: { fontSize: 12, color: '#aaa' },
  companionResult: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#fff', borderRadius: 10, padding: 10, gap: 10, borderWidth: 0.5, borderColor: '#EAE6E0' },
  companionAvatar: { width: 32, height: 32, borderRadius: 16, backgroundColor: '#FAEEDA', alignItems: 'center', justifyContent: 'center' },
  companionAvatarText: { fontSize: 11, fontWeight: '800', color: '#633806' },
  companionUsername: { flex: 1, fontSize: 13, color: '#1a1a1a' },
  companionAdd: { fontSize: 12, color: '#D95F2B', fontWeight: '600' },
});

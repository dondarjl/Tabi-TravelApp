import { useRouter } from 'expo-router';
import { useEffect, useState } from 'react';
import { ActivityIndicator, FlatList, Image, SafeAreaView, ScrollView, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { supabase } from '../../services/supabase';

const TIPOS = ['Urbano', 'Cultural', 'Naturaleza', 'Gastronómico', 'Fiesta', 'Playa', 'Aventura', 'Relax', 'Road trip', 'Mochilero'];
const CONTINENTES = ['Europa', 'Asia', 'América', 'África', 'Oceanía'];
const PRECIOS = [
  { label: 'Económico', max: 500 },
  { label: 'Medio', min: 500, max: 1500 },
  { label: 'Alto', min: 1500, max: 3000 },
  { label: 'Lujo', min: 3000 },
];
const DURACIONES = [
  { label: 'Escapada', min: 1, max: 3 },
  { label: 'Semana', min: 4, max: 7 },
  { label: 'Dos semanas', min: 8, max: 14 },
  { label: 'Viaje largo', min: 15 },
];

const PAISES_POR_CONTINENTE: Record<string, string[]> = {
  Europa: ['España', 'Francia', 'Italia', 'Portugal', 'Grecia', 'Alemania', 'Reino Unido', 'Países Bajos', 'Suecia', 'Noruega', 'Islandia', 'Suiza', 'Austria', 'Croacia', 'Polonia'],
  Asia: ['Japón', 'Tailandia', 'Vietnam', 'Indonesia', 'India', 'China', 'Corea del Sur', 'Filipinas', 'Camboya', 'Nepal', 'Sri Lanka', 'Turquía', 'Georgia'],
  América: ['México', 'Colombia', 'Argentina', 'Perú', 'Brasil', 'Chile', 'Cuba', 'Costa Rica', 'Estados Unidos', 'Canadá', 'Ecuador', 'Bolivia'],
  África: ['Marruecos', 'Tanzania', 'Sudáfrica', 'Kenia', 'Egipto', 'Etiopía', 'Senegal', 'Madagascar'],
  Oceanía: ['Australia', 'Nueva Zelanda', 'Fiji', 'Bali'],
};

export default function DiscoverScreen() {
  const router = useRouter();
  const [trips, setTrips] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [selectedTipos, setSelectedTipos] = useState<string[]>([]);
  const [selectedPrecio, setSelectedPrecio] = useState<string | null>(null);
  const [selectedDuracion, setSelectedDuracion] = useState<string | null>(null);
  const [selectedContinente, setSelectedContinente] = useState<string | null>(null);

  useEffect(() => {
    supabase
      .from('trips')
      .select('*')
      .eq('is_published', true)
      .order('created_at', { ascending: false })
      .then(({ data, error }) => {
        if (!error && data) setTrips(data);
        setLoading(false);
      });
  }, []);

  function toggleTipo(tipo: string) {
    setSelectedTipos(prev =>
      prev.includes(tipo) ? prev.filter(t => t !== tipo) : [...prev, tipo]
    );
  }

  const filtered = trips.filter(t => {
    const matchSearch = !search ||
      t.destination?.toLowerCase().includes(search.toLowerCase()) ||
      t.title?.toLowerCase().includes(search.toLowerCase());

    const matchTipo = selectedTipos.length === 0 ||
      selectedTipos.some(tipo => t.trip_type?.includes(tipo));

    const precio = t.total_cost || 0;
    const precioObj = PRECIOS.find(p => p.label === selectedPrecio);
    const matchPrecio = !selectedPrecio || (
      (!precioObj?.min || precio >= precioObj.min) &&
      (!precioObj?.max || precio < precioObj.max)
    );

    const dias = t.start_date && t.end_date
      ? Math.round((new Date(t.end_date).getTime() - new Date(t.start_date).getTime()) / (1000 * 60 * 60 * 24))
      : null;
    const durObj = DURACIONES.find(d => d.label === selectedDuracion);
    const matchDuracion = !selectedDuracion || !dias || (
      (!durObj?.min || dias >= durObj.min) &&
      (!durObj?.max || dias <= durObj.max)
    );

    const matchContinente = !selectedContinente || (() => {
      const paises = PAISES_POR_CONTINENTE[selectedContinente] || [];
      return paises.some(pais =>
        t.destination?.toLowerCase().includes(pais.toLowerCase())
      );
    })();

    return matchSearch && matchTipo && matchPrecio && matchDuracion && matchContinente;
  });

  const hasFilters = selectedTipos.length > 0 || selectedPrecio || selectedDuracion || selectedContinente;

  function resetFilters() {
    setSelectedTipos([]);
    setSelectedPrecio(null);
    setSelectedDuracion(null);
    setSelectedContinente(null);
    setSearch('');
  }

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.logo}>descubrir</Text>
        {hasFilters && (
          <TouchableOpacity onPress={resetFilters}>
            <Text style={styles.resetBtn}>Limpiar</Text>
          </TouchableOpacity>
        )}
      </View>

      {/* Búsqueda */}
      <View style={styles.searchBar}>
        <Text style={styles.searchIcon}>🔍</Text>
        <TextInput
          style={styles.searchInput}
          placeholder="Buscar destino..."
          placeholderTextColor="#bbb"
          value={search}
          onChangeText={setSearch}
        />
        {search.length > 0 && (
          <TouchableOpacity onPress={() => setSearch('')}>
            <Text style={styles.clearBtn}>✕</Text>
          </TouchableOpacity>
        )}
      </View>

      <ScrollView style={styles.filtersContainer} showsVerticalScrollIndicator={false}>

        {/* Continente */}
        <Text style={styles.filterLabel}>🌍 Continente</Text>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.chipsRow}>
          {CONTINENTES.map(c => (
            <TouchableOpacity
              key={c}
              style={[styles.chip, selectedContinente === c && styles.chipActive]}
              onPress={() => setSelectedContinente(selectedContinente === c ? null : c)}
            >
              <Text style={[styles.chipText, selectedContinente === c && styles.chipTextActive]}>{c}</Text>
            </TouchableOpacity>
          ))}
        </ScrollView>

        {/* Tipo de viaje */}
        <Text style={styles.filterLabel}>🧭 Tipo de viaje</Text>
        <View style={styles.chipsWrap}>
          {TIPOS.map(t => (
            <TouchableOpacity
              key={t}
              style={[styles.chip, selectedTipos.includes(t) && styles.chipActive]}
              onPress={() => toggleTipo(t)}
            >
              <Text style={[styles.chipText, selectedTipos.includes(t) && styles.chipTextActive]}>{t}</Text>
            </TouchableOpacity>
          ))}
        </View>

        {/* Precio */}
        <Text style={styles.filterLabel}>💰 Presupuesto</Text>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.chipsRow}>
          {PRECIOS.map(p => (
            <TouchableOpacity
              key={p.label}
              style={[styles.chip, selectedPrecio === p.label && styles.chipActive]}
              onPress={() => setSelectedPrecio(selectedPrecio === p.label ? null : p.label)}
            >
              <Text style={[styles.chipText, selectedPrecio === p.label && styles.chipTextActive]}>{p.label}</Text>
            </TouchableOpacity>
          ))}
        </ScrollView>

        {/* Duración */}
        <Text style={styles.filterLabel}>📅 Duración</Text>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.chipsRow}>
          {DURACIONES.map(d => (
            <TouchableOpacity
              key={d.label}
              style={[styles.chip, selectedDuracion === d.label && styles.chipActive]}
              onPress={() => setSelectedDuracion(selectedDuracion === d.label ? null : d.label)}
            >
              <Text style={[styles.chipText, selectedDuracion === d.label && styles.chipTextActive]}>{d.label}</Text>
            </TouchableOpacity>
          ))}
        </ScrollView>

        {/* Resultados */}
        <Text style={styles.resultCount}>{filtered.length} viajes encontrados</Text>

        {loading ? (
          <ActivityIndicator color="#D95F2B" style={{ marginTop: 20 }} />
        ) : filtered.length === 0 ? (
          <View style={styles.emptyState}>
            <Text style={styles.emptyEmoji}>🔍</Text>
            <Text style={styles.emptyTitle}>Sin resultados</Text>
            <Text style={styles.emptySubtitle}>Prueba con otros filtros</Text>
          </View>
        ) : (
          <FlatList
            data={filtered}
            keyExtractor={item => item.id}
            numColumns={2}
            scrollEnabled={false}
            contentContainerStyle={styles.grid}
            columnWrapperStyle={{ gap: 8 }}
            renderItem={({ item }) => (
              <TouchableOpacity
                style={styles.cell}
                onPress={() => router.push(`/trip/${item.id}` as any)}
              >
                <View style={styles.cellImg}>
                  {item.cover_photo_url ? (
                    <Image
                      source={{ uri: item.cover_photo_url }}
                      style={{ width: '100%', height: '100%' }}
                      resizeMode="cover"
                    />
                  ) : (
                    <Text style={styles.cellEmoji}>🌍</Text>
                  )}
                </View>
                <View style={styles.cellBody}>
                  <Text style={styles.cellDest} numberOfLines={1}>{item.destination}</Text>
                  <Text style={styles.cellTitle} numberOfLines={1}>{item.title}</Text>
                  <Text style={styles.cellPrice}>€{item.total_cost}</Text>
                </View>
              </TouchableOpacity>
            )}
          />
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#F5F2ED' },
  header: { paddingHorizontal: 20, paddingVertical: 16, backgroundColor: '#fff', borderBottomWidth: 0.5, borderBottomColor: '#E0DDD8', flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  logo: { fontSize: 20, fontWeight: '800', color: '#1a1a1a' },
  resetBtn: { fontSize: 12, color: '#D95F2B', fontWeight: '600' },
  searchBar: { flexDirection: 'row', alignItems: 'center', margin: 12, backgroundColor: '#fff', borderRadius: 12, paddingHorizontal: 12, paddingVertical: 10, borderWidth: 0.5, borderColor: '#EAE6E0', gap: 8 },
  searchIcon: { fontSize: 14 },
  searchInput: { flex: 1, fontSize: 14, color: '#1a1a1a' },
  clearBtn: { fontSize: 12, color: '#bbb' },
  filtersContainer: { flex: 1 },
  filterLabel: { fontSize: 11, color: '#888', fontWeight: '600', textTransform: 'uppercase', letterSpacing: 0.5, paddingHorizontal: 14, marginTop: 14, marginBottom: 6 },
  chipsRow: { paddingHorizontal: 12, gap: 6 },
  chipsWrap: { flexDirection: 'row', flexWrap: 'wrap', paddingHorizontal: 12, gap: 6 },
  chip: { paddingHorizontal: 14, paddingVertical: 6, borderRadius: 20, borderWidth: 0.5, borderColor: '#E0DDD8', backgroundColor: '#fff' },
  chipActive: { backgroundColor: '#1a1a1a', borderColor: '#1a1a1a' },
  chipText: { fontSize: 12, color: '#888' },
  chipTextActive: { color: '#fff' },
  resultCount: { fontSize: 11, color: '#bbb', paddingHorizontal: 14, marginTop: 16, marginBottom: 4 },
  grid: { padding: 10, gap: 8 },
  cell: { flex: 1, backgroundColor: '#fff', borderRadius: 12, overflow: 'hidden', borderWidth: 0.5, borderColor: '#EAE6E0' },
  cellImg: { height: 90, alignItems: 'center', justifyContent: 'center', backgroundColor: '#F5F2ED' },
  cellEmoji: { fontSize: 32 },
  cellBody: { padding: 8, gap: 2 },
  cellDest: { fontSize: 12, fontWeight: '700', color: '#1a1a1a' },
  cellTitle: { fontSize: 10, color: '#aaa' },
  cellPrice: { fontSize: 11, color: '#D95F2B', marginTop: 2 },
  emptyState: { alignItems: 'center', padding: 40, gap: 8 },
  emptyEmoji: { fontSize: 40 },
  emptyTitle: { fontSize: 16, fontWeight: '700', color: '#1a1a1a' },
  emptySubtitle: { fontSize: 13, color: '#aaa' },
});
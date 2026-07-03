import { useRouter } from 'expo-router';
import { useEffect, useState } from 'react';
import {
    Alert,
    SafeAreaView,
    ScrollView,
    StyleSheet,
    Switch,
    Text,
    TouchableOpacity,
    View,
} from 'react-native';
import { supabase } from '../services/supabase';

type SettingRowProps = {
  label: string;
  value?: string;
  onPress?: () => void;
  danger?: boolean;
  toggle?: boolean;
  toggleValue?: boolean;
  onToggle?: (v: boolean) => void;
  soon?: boolean;
};

function SettingRow({ label, value, onPress, danger, toggle, toggleValue, onToggle, soon }: SettingRowProps) {
  return (
    <TouchableOpacity
      style={styles.row}
      onPress={soon ? () => Alert.alert('Próximamente', 'Esta función estará disponible próximamente.') : onPress}
      disabled={toggle && !soon}
      activeOpacity={toggle && !soon ? 1 : 0.7}
    >
      <Text style={[styles.rowLabel, danger && styles.rowLabelDanger]}>{label}</Text>
      <View style={styles.rowRight}>
        {soon && <Text style={styles.soonBadge}>Próximamente</Text>}
        {value && !soon && <Text style={styles.rowValue}>{value}</Text>}
        {toggle && !soon && (
          <Switch
            value={toggleValue}
            onValueChange={onToggle}
            trackColor={{ false: '#E0DDD8', true: '#D95F2B' }}
            thumbColor="#fff"
          />
        )}
        {!toggle && !soon && <Text style={styles.chevron}>›</Text>}
        {danger && !soon && <Text style={[styles.chevron, styles.chevronDanger]}>›</Text>}
      </View>
    </TouchableOpacity>
  );
}

function SectionHeader({ title }: { title: string }) {
  return <Text style={styles.sectionHeader}>{title}</Text>;
}

function Divider() {
  return <View style={styles.divider} />;
}

export default function SettingsScreen() {
  const router = useRouter();
  const [isPrivate, setIsPrivate] = useState(false);
  const [userId, setUserId] = useState<string | null>(null);

  useEffect(() => {
    async function load() {
      const { data: { user: authUser } } = await supabase.auth.getUser();
      if (!authUser) return;
      setUserId(authUser.id);

      const { data } = await supabase
        .from('users')
        .select('is_private')
        .eq('id', authUser.id)
        .single();

      if (data) setIsPrivate(data.is_private ?? false);
    }
    load();
  }, []);

  async function handlePrivacyToggle(value: boolean) {
    setIsPrivate(value);
    if (!userId) return;
    await supabase
      .from('users')
      .update({ is_private: value })
      .eq('id', userId);
  }

  async function handleLogout() {
    Alert.alert(
      'Cerrar sesión',
      '¿Seguro que quieres salir?',
      [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Salir',
          style: 'destructive',
          onPress: async () => {
            await supabase.auth.signOut();
            router.replace('/login' as any);
          },
        },
      ]
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      {/* Header */}
      <View style={styles.topBar}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
          <Text style={styles.backText}>←</Text>
        </TouchableOpacity>
        <Text style={styles.topBarTitle}>Ajustes</Text>
        <View style={{ width: 32 }} />
      </View>

      <ScrollView contentContainerStyle={styles.body}>

        {/* CUENTA */}
        <SectionHeader title="Cuenta" />
        <View style={styles.card}>
          <SettingRow
            label="Cuenta privada"
            toggle
            toggleValue={isPrivate}
            onToggle={handlePrivacyToggle}
          />
          <Divider />
          <SettingRow label="Cuentas bloqueadas" soon />
          <Divider />
          <SettingRow label="Estadísticas históricas" soon />
        </View>

        {/* PAGOS */}
        <SectionHeader title="Pagos" />
        <View style={styles.card}>
          <SettingRow label="Métodos de pago" soon />
          <Divider />
          <SettingRow label="Historial de pagos" soon />
        </View>

        {/* PREFERENCIAS */}
        <SectionHeader title="Preferencias" />
        <View style={styles.card}>
          <SettingRow label="Idioma" soon />
          <Divider />
          <SettingRow label="Notificaciones" soon />
          <Divider />
          <SettingRow label="Modo oscuro" soon />
        </View>

        {/* SESIÓN */}
        <SectionHeader title="Sesión" />
        <View style={styles.card}>
          <SettingRow
            label="Cerrar sesión"
            onPress={handleLogout}
          />
        </View>

        {/* ZONA PELIGROSA */}
        <SectionHeader title="Zona de peligro" />
        <View style={styles.card}>
          <SettingRow
            label="Eliminar cuenta"
            danger
            soon
          />
        </View>

        <Text style={styles.version}>tabi · v0.1.0</Text>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#F5F2ED' },
  topBar: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
    paddingHorizontal: 16, paddingVertical: 12,
    backgroundColor: '#fff', borderBottomWidth: 0.5, borderBottomColor: '#F0EDE8',
  },
  backBtn: { width: 32, height: 32, borderRadius: 16, backgroundColor: '#F5F2ED', alignItems: 'center', justifyContent: 'center' },
  backText: { fontSize: 16, color: '#1a1a1a' },
  topBarTitle: { fontSize: 15, fontWeight: '700', color: '#1a1a1a' },
  body: { padding: 16, gap: 4, paddingBottom: 40 },
  sectionHeader: { fontSize: 11, fontWeight: '700', color: '#aaa', textTransform: 'uppercase', letterSpacing: 0.8, marginTop: 16, marginBottom: 6, paddingHorizontal: 4 },
  card: { backgroundColor: '#fff', borderRadius: 14, borderWidth: 0.5, borderColor: '#EAE6E0', overflow: 'hidden' },
  row: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 16, paddingVertical: 14 },
  rowLabel: { fontSize: 14, color: '#1a1a1a', fontWeight: '500' },
  rowLabelDanger: { color: '#E0443A' },
  rowRight: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  rowValue: { fontSize: 13, color: '#aaa' },
  chevron: { fontSize: 20, color: '#ccc', lineHeight: 22 },
  chevronDanger: { color: '#E0443A' },
  soonBadge: { fontSize: 10, color: '#D95F2B', backgroundColor: '#FFF4EF', paddingHorizontal: 8, paddingVertical: 2, borderRadius: 10, fontWeight: '600', overflow: 'hidden' },
  divider: { height: 0.5, backgroundColor: '#F0EDE8', marginLeft: 16 },
  version: { textAlign: 'center', fontSize: 11, color: '#ccc', marginTop: 24 },
});

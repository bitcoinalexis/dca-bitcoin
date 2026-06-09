import streamlit as st
import sqlite3
import pandas as pd
import requests
from datetime import datetime
import pytz
import plotly.express as px
import plotly.graph_objects as go
import os
import sys
import subprocess

#  cd "/home/alexis/AngyLabs/DCA BITCOIN" && streamlit run app.py

st.set_page_config(
    page_title="DCA Crypto Tracker",
    page_icon="₿",
    layout="wide",
    initial_sidebar_state="expanded",
)

CDMX_TZ = pytz.timezone("America/Mexico_City")
DB_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "dca_bitcoin.db")


def get_conn():
    return sqlite3.connect(DB_PATH)


def _col_exists(conn, table, col):
    rows = conn.execute(f"PRAGMA table_info({table})").fetchall()
    return any(r[1] == col for r in rows)


def init_db():
    with get_conn() as conn:
        conn.execute("""
            CREATE TABLE IF NOT EXISTS dca_records (
                id                    INTEGER PRIMARY KEY AUTOINCREMENT,
                fecha                 TEXT    NOT NULL,
                mxn_gastado           REAL    NOT NULL,
                tipo_cambio_mxn_usd   REAL    NOT NULL,
                usd_equivalente       REAL    NOT NULL,
                btc_adquirido         REAL    NOT NULL,
                precio_compra_btc_usd REAL    NOT NULL,
                moneda_gasto          TEXT,
                notas                 TEXT
            )
        """)
        conn.execute("""
            CREATE TABLE IF NOT EXISTS fee_records (
                id                INTEGER PRIMARY KEY AUTOINCREMENT,
                fecha             TEXT    NOT NULL,
                btc_fee           REAL    NOT NULL,
                precio_btc_usd    REAL    NOT NULL,
                usd_fee           REAL    NOT NULL,
                tipo_movimiento   TEXT    NOT NULL,
                notas             TEXT
            )
        """)
        conn.execute("""
            CREATE TABLE IF NOT EXISTS sol_records (
                id                    INTEGER PRIMARY KEY AUTOINCREMENT,
                fecha                 TEXT    NOT NULL,
                mxn_gastado           REAL    NOT NULL,
                tipo_cambio_mxn_usd   REAL    NOT NULL,
                usd_equivalente       REAL    NOT NULL,
                sol_adquirido         REAL    NOT NULL,
                precio_compra_sol_usd REAL    NOT NULL,
                moneda_gasto          TEXT,
                notas                 TEXT
            )
        """)
        if not _col_exists(conn, "dca_records", "moneda_gasto"):
            conn.execute("ALTER TABLE dca_records ADD COLUMN moneda_gasto TEXT")
        conn.commit()


@st.cache_data(ttl=300)
def obtener_tipo_cambio() -> float | None:
    endpoints = [
        "https://open.er-api.com/v6/latest/USD",
        "https://api.exchangerate-api.com/v4/latest/USD",
    ]
    for url in endpoints:
        try:
            r = requests.get(url, timeout=6)
            if r.status_code == 200:
                rates = r.json().get("rates", {})
                if "MXN" in rates:
                    return float(rates["MXN"])
        except Exception:
            continue
    return None


@st.cache_data(ttl=120)
def obtener_precio_btc() -> float | None:
    try:
        r = requests.get(
            "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=usd",
            timeout=6,
        )
        if r.status_code == 200:
            return float(r.json()["bitcoin"]["usd"])
    except Exception:
        pass
    return None


@st.cache_data(ttl=120)
def obtener_precio_sol() -> float | None:
    try:
        r = requests.get(
            "https://api.coingecko.com/api/v3/simple/price?ids=solana&vs_currencies=usd",
            timeout=6,
        )
        if r.status_code == 200:
            return float(r.json()["solana"]["usd"])
    except Exception:
        pass
    return None


def insertar_registro_btc(mxn, tc, usd, btc, precio, moneda_gasto, notas):
    fecha = datetime.now(CDMX_TZ).strftime("%Y-%m-%d %H:%M:%S")
    with get_conn() as conn:
        conn.execute(
            """INSERT INTO dca_records
               (fecha, mxn_gastado, tipo_cambio_mxn_usd, usd_equivalente,
                btc_adquirido, precio_compra_btc_usd, moneda_gasto, notas)
               VALUES (?,?,?,?,?,?,?,?)""",
            (fecha, mxn, tc, usd, btc, precio, moneda_gasto, notas),
        )
        conn.commit()


def obtener_registros_btc() -> pd.DataFrame:
    with get_conn() as conn:
        return pd.read_sql_query("SELECT * FROM dca_records ORDER BY fecha DESC", conn)


def eliminar_registro_btc(record_id: int):
    with get_conn() as conn:
        conn.execute("DELETE FROM dca_records WHERE id = ?", (record_id,))
        conn.commit()


def actualizar_registro_btc(record_id, mxn, tc, btc, precio_btc_usd, moneda_gasto, notas):
    usd = mxn / tc if tc > 0 else 0.0
    with get_conn() as conn:
        conn.execute("""
            UPDATE dca_records
            SET mxn_gastado = ?, tipo_cambio_mxn_usd = ?, usd_equivalente = ?,
                btc_adquirido = ?, precio_compra_btc_usd = ?, moneda_gasto = ?, notas = ?
            WHERE id = ?
        """, (mxn, tc, usd, btc, precio_btc_usd, moneda_gasto, notas, record_id))
        conn.commit()


def insertar_fee(btc_fee, precio_btc_usd, tipo_movimiento, notas):
    fecha = datetime.now(CDMX_TZ).strftime("%Y-%m-%d %H:%M:%S")
    usd_fee = btc_fee * precio_btc_usd
    with get_conn() as conn:
        conn.execute(
            """INSERT INTO fee_records
               (fecha, btc_fee, precio_btc_usd, usd_fee, tipo_movimiento, notas)
               VALUES (?,?,?,?,?,?)""",
            (fecha, btc_fee, precio_btc_usd, usd_fee, tipo_movimiento, notas),
        )
        conn.commit()


def obtener_fees() -> pd.DataFrame:
    with get_conn() as conn:
        return pd.read_sql_query("SELECT * FROM fee_records ORDER BY fecha DESC", conn)


def eliminar_fee(fee_id: int):
    with get_conn() as conn:
        conn.execute("DELETE FROM fee_records WHERE id = ?", (fee_id,))
        conn.commit()


def insertar_registro_sol(mxn, tc, usd, sol, precio, moneda_gasto, notas):
    fecha = datetime.now(CDMX_TZ).strftime("%Y-%m-%d %H:%M:%S")
    with get_conn() as conn:
        conn.execute(
            """INSERT INTO sol_records
               (fecha, mxn_gastado, tipo_cambio_mxn_usd, usd_equivalente,
                sol_adquirido, precio_compra_sol_usd, moneda_gasto, notas)
               VALUES (?,?,?,?,?,?,?,?)""",
            (fecha, mxn, tc, usd, sol, precio, moneda_gasto, notas),
        )
        conn.commit()


def obtener_registros_sol() -> pd.DataFrame:
    with get_conn() as conn:
        return pd.read_sql_query("SELECT * FROM sol_records ORDER BY fecha DESC", conn)


def eliminar_registro_sol(record_id: int):
    with get_conn() as conn:
        conn.execute("DELETE FROM sol_records WHERE id = ?", (record_id,))
        conn.commit()


def exportar_db_bytes() -> bytes:
    with get_conn() as conn:
        conn.commit()
    with open(DB_PATH, "rb") as f:
        return f.read()


def importar_db_bytes(data: bytes) -> tuple[bool, str]:
    import tempfile, shutil
    REQUIRED_TABLES = {"dca_records", "fee_records", "sol_records"}
    tmp_path = None
    try:
        with tempfile.NamedTemporaryFile(delete=False, suffix=".db") as tmp:
            tmp.write(data)
            tmp_path = tmp.name
        conn = sqlite3.connect(tmp_path)
        try:
            rows = conn.execute("SELECT name FROM sqlite_master WHERE type='table'").fetchall()
            tablas = {r[0] for r in rows}
            faltantes = REQUIRED_TABLES - tablas
            if faltantes:
                return False, f"Archivo invalido. Faltan tablas: {', '.join(sorted(faltantes))}"
        finally:
            conn.close()
        if os.path.exists(DB_PATH):
            backup_path = DB_PATH + f".backup_{datetime.now(CDMX_TZ).strftime('%Y%m%d_%H%M%S')}"
            shutil.copy2(DB_PATH, backup_path)
        shutil.move(tmp_path, DB_PATH)
        tmp_path = None
        return True, "Base de datos importada correctamente. Recarga la pagina."
    except sqlite3.DatabaseError:
        return False, "El archivo no es una base de datos SQLite valida."
    except Exception as e:
        return False, f"Error al importar: {e}"
    finally:
        if tmp_path and os.path.exists(tmp_path):
            try:
                os.unlink(tmp_path)
            except Exception:
                pass


def inyectar_css():
    st.markdown("""
        <style>
        section[data-testid="stSidebar"] { background-color: #1a1a2e; }
        section[data-testid="stSidebar"] * { color: #e0e0e0 !important; }

        div[data-testid="metric-container"] {
            background: #1e1e2e;
            border: 1px solid #f7931a33;
            border-radius: 10px;
            padding: 12px 16px;
        }
        div[data-testid="metric-container"] label { color: #aaaaaa !important; }
        div[data-testid="metric-container"] div[data-testid="stMetricValue"] {
            color: #f7931a !important;
            font-size: 1.3rem !important;
        }

        div.stButton > button[kind="primary"] {
            background-color: #f7931a;
            color: #000;
            font-weight: 700;
            border: none;
        }
        div.stButton > button[kind="primary"]:hover { background-color: #e07d0a; }

        h1 { color: #f7931a !important; }
        </style>
    """, unsafe_allow_html=True)


init_db()
inyectar_css()

tc_api = obtener_tipo_cambio()

with st.sidebar:
    st.image("https://upload.wikimedia.org/wikipedia/commons/4/46/Bitcoin.svg", width=60)
    st.title("DCA Crypto")
    st.caption("Registro de acumulacion de criptomonedas")
    st.markdown("---")
    seccion = st.radio(
        "Selecciona activo:",
        options=["Bitcoin (BTC)", "Solana (SOL)"],
        index=0,
    )
    st.markdown("---")
    st.markdown("""
        **Que es DCA?**
        *Dollar Cost Averaging* — estrategia de compra periodica para
        reducir el impacto de la volatilidad del precio.
    """)
    st.markdown("---")
    with st.expander("Backup / Restaurar BD", expanded=False):
        st.caption("Exporta toda la base de datos (BTC, SOL y fees) o impórtala en otra computadora.")
        nombre_export = f"dca_bitcoin_{datetime.now(CDMX_TZ).strftime('%Y%m%d_%H%M%S')}.db"
        st.download_button(
            "Exportar base de datos",
            data=exportar_db_bytes(),
            file_name=nombre_export,
            mime="application/octet-stream",
            width="stretch",
            key="btn_export_db",
        )
        st.markdown("**Importar base de datos**")
        archivo = st.file_uploader("Selecciona un archivo .db", type=["db", "sqlite", "sqlite3"], key="db_uploader")
        confirmar = st.checkbox("Entiendo que se reemplazará la base de datos actual (se crea un backup automático).", key="chk_import")
        if st.button("Importar ahora", width="stretch", disabled=not (archivo and confirmar), key="btn_import_db"):
            ok, msg = importar_db_bytes(archivo.getvalue())
            if ok:
                st.success(msg)
                st.cache_data.clear()
                st.rerun()
            else:
                st.error(msg)

    st.markdown("---")
    st.caption("Zona horaria: CDMX (America/Mexico_City)")
    ahora_cdmx = datetime.now(CDMX_TZ)
    st.caption(f"Hora actual: **{ahora_cdmx.strftime('%d/%m/%Y %H:%M:%S')}**")


if seccion == "Bitcoin (BTC)":
    st.title("Panel de Registro DCA — Bitcoin (BTC)")
    st.markdown("Registra tus compras de Bitcoin y lleva el control de tu estrategia DCA.")
    st.markdown("---")

    tab_registro, tab_fees, tab_historial, tab_resumen = st.tabs(
        ["Registrar Compra", "Fees de Envio", "Historial", "Resumen & Graficas"]
    )

    with tab_registro:
        st.subheader("Nueva compra de Bitcoin")

        if tc_api:
            st.success(f"Tipo de cambio: **1 USD = {tc_api:.4f} MXN** (actualizacion cada 5 min)")
        else:
            st.warning("No se pudo obtener el tipo de cambio. Ingresa el valor manualmente.")

        col_izq, col_der = st.columns(2, gap="large")

        with col_izq:
            st.markdown("#### Datos en pesos MXN")
            tipo_cambio = st.number_input(
                "Tipo de cambio (MXN por 1 USD)",
                min_value=0.01,
                value=float(tc_api) if tc_api else 17.50,
                step=0.01,
                format="%.4f",
            )
            moneda_gasto = st.radio(
                "Registrar gasto en:",
                options=["MXN", "USD (USDC/USDT)"],
                horizontal=True,
                key="btc_moneda_gasto",
            )
            if moneda_gasto == "MXN":
                mxn_gastado = st.number_input("MXN gastados", min_value=0.0, value=0.0, step=100.0, format="%.2f")
                usd_equivalente = mxn_gastado / tipo_cambio if tipo_cambio > 0 else 0.0
            else:
                usd_input = st.number_input("USD gastados (USDC/USDT)", min_value=0.0, value=0.0, step=5.0, format="%.4f")
                usd_equivalente = float(usd_input)
                mxn_gastado = usd_input * tipo_cambio if tipo_cambio > 0 else 0.0
                if usd_input > 0:
                    st.info(f"Equivalente en MXN: **${mxn_gastado:,.2f} MXN**")
            st.info(f"Equivalente en USD: **${usd_equivalente:,.4f} USD**" if usd_equivalente > 0 else "Ingresa el monto.")

        with col_der:
            st.markdown("#### Datos Bitcoin")
            btc_adquirido = st.number_input("BTC adquiridos", min_value=0.0, value=0.0, step=0.00001, format="%.8f")
            modo_precio = st.radio("Ingresar precio BTC en:", options=["MXN", "USD"], horizontal=True, key="btc_modo_precio")
            if modo_precio == "MXN":
                precio_btc_mxn_input = st.number_input("Precio de compra BTC (MXN)", min_value=0.0, value=0.0, step=1000.0, format="%.2f")
                precio_btc_usd = precio_btc_mxn_input / tipo_cambio if tipo_cambio > 0 and precio_btc_mxn_input > 0 else 0.0
                if precio_btc_mxn_input > 0:
                    st.info(f"Equivalente en USD: **${precio_btc_usd:,.2f} USD**")
            else:
                precio_btc_usd = st.number_input("Precio de compra BTC (USD)", min_value=0.0, value=0.0, step=100.0, format="%.2f")
                if precio_btc_usd > 0:
                    st.info(f"Equivalente en MXN: **${precio_btc_usd * tipo_cambio:,.2f} MXN**")

        notas = st.text_area("Notas (opcional)", placeholder="Ej: Compra automatica Bitso, semana 12...", height=80)

        if mxn_gastado > 0 or btc_adquirido > 0:
            st.markdown("---")
            st.markdown("**Vista previa:**")
            p1, p2, p3, p4 = st.columns(4)
            p1.metric("MXN gastados", f"${mxn_gastado:,.2f}")
            p2.metric("USD equivalente", f"${usd_equivalente:,.4f}")
            p3.metric("BTC adquiridos", f"{btc_adquirido:.8f}")
            p4.metric("Precio BTC (USD)", f"${precio_btc_usd:,.2f}")

        st.markdown("---")
        if st.button("Guardar registro BTC", type="primary", width="stretch"):
            errores = []
            if usd_equivalente <= 0:
                errores.append("El monto gastado debe ser mayor a 0.")
            if btc_adquirido <= 0:
                errores.append("La cantidad de BTC debe ser mayor a 0.")
            if precio_btc_usd <= 0:
                errores.append("El precio de compra del BTC debe ser mayor a 0.")
            if errores:
                for e in errores:
                    st.error(e)
            else:
                insertar_registro_btc(mxn_gastado, tipo_cambio, usd_equivalente, btc_adquirido, precio_btc_usd,
                                      "USD" if moneda_gasto != "MXN" else "MXN", notas)
                st.success("Registro guardado exitosamente!")
                st.balloons()

    with tab_fees:
        st.subheader("Registrar fee de envio de Bitcoin")
        st.caption("Registra los fees (comisiones) pagados al enviar BTC.")

        col_f1, col_f2 = st.columns(2, gap="large")
        with col_f1:
            st.markdown("#### Fee pagado")
            btc_fee = st.number_input("BTC pagados como fee", min_value=0.0, value=0.0, step=0.000001, format="%.8f", key="btc_fee_input")
            precio_btc_fee = st.number_input("Precio BTC al momento del envio (USD)", min_value=0.0, value=0.0, step=100.0, format="%.2f", key="precio_btc_fee_input")
            usd_fee_calc = btc_fee * precio_btc_fee
            mxn_fee_calc = usd_fee_calc * (tc_api if tc_api else 0)
            if btc_fee > 0 and precio_btc_fee > 0:
                st.info(f"Fee equivalente: **${usd_fee_calc:.6f} USD**" + (f"  aprox.  **${mxn_fee_calc:.4f} MXN**" if tc_api else ""))

        with col_f2:
            st.markdown("#### Detalle del movimiento")
            tipo_movimiento = st.selectbox("Tipo de movimiento", options=["Envio a wallet fria", "Retiro de exchange", "Consolidacion de UTXOs", "Envio entre wallets propias", "Pago a tercero", "Otro"], key="tipo_mov_input")
            notas_fee = st.text_area("Notas (opcional)", placeholder="Ej: Envio de Bitso a Ledger, tx: abc123...", height=80, key="notas_fee_input")

        if btc_fee > 0 and precio_btc_fee > 0:
            st.markdown("---")
            pf1, pf2, pf3 = st.columns(3)
            pf1.metric("BTC fee", f"{btc_fee:.8f} BTC")
            pf2.metric("USD equivalente", f"${usd_fee_calc:.6f}")
            pf3.metric("Tipo de movimiento", tipo_movimiento)

        st.markdown("---")
        if st.button("Guardar fee", type="primary", width="stretch", key="btn_save_fee"):
            if btc_fee <= 0 or precio_btc_fee <= 0:
                st.error("Ingresa valores validos para el fee.")
            else:
                insertar_fee(btc_fee, precio_btc_fee, tipo_movimiento, notas_fee)
                st.success("Fee registrado correctamente!")
                st.balloons()

        st.markdown("---")
        st.subheader("Historial de fees registrados")
        df_fees = obtener_fees()
        if df_fees.empty:
            st.info("Aun no hay fees registrados.")
        else:
            df_fees_vis = df_fees.rename(columns={"id": "ID", "fecha": "Fecha (CDMX)", "btc_fee": "BTC Fee", "precio_btc_usd": "Precio BTC (USD)", "usd_fee": "USD Fee", "tipo_movimiento": "Tipo", "notas": "Notas"})
            st.dataframe(df_fees_vis.style.format({"BTC Fee": "{:.8f}", "Precio BTC (USD)": "${:,.2f}", "USD Fee": "${:.6f}"}), width="stretch", hide_index=True)
            sf1, sf2, sf3 = st.columns(3)
            sf1.metric("Total BTC en fees", f"{df_fees['btc_fee'].sum():.8f}")
            sf2.metric("Total USD en fees", f"${df_fees['usd_fee'].sum():.6f}")
            sf3.metric("Operaciones", str(len(df_fees)))
            st.download_button("Descargar fees CSV", df_fees.to_csv(index=False).encode("utf-8"), "dca_bitcoin_fees.csv", "text/csv", width="stretch")
            st.markdown("---")
            with st.expander("Eliminar un fee"):
                ids_fees = df_fees["id"].tolist()
                id_fee_del = st.selectbox("ID del fee a eliminar", options=ids_fees,
                    format_func=lambda x: f"ID {x} — {df_fees.loc[df_fees['id']==x, 'fecha'].values[0]} | {df_fees.loc[df_fees['id']==x, 'tipo_movimiento'].values[0]}",
                    key="sel_fee_del")
                if st.button("Eliminar fee seleccionado", type="secondary", key="btn_del_fee"):
                    eliminar_fee(id_fee_del)
                    st.success(f"Fee ID {id_fee_del} eliminado.")
                    st.rerun()

    with tab_historial:
        st.subheader("Historial de compras BTC")
        df = obtener_registros_btc()
        if df.empty:
            st.info("No hay registros aun.")
        else:
            df_vis = df.rename(columns={"id": "ID", "fecha": "Fecha (CDMX)", "mxn_gastado": "MXN Gastados",
                "tipo_cambio_mxn_usd": "T.C. MXN/USD", "usd_equivalente": "USD Equiv.",
                "btc_adquirido": "BTC Adquiridos", "precio_compra_btc_usd": "Precio BTC (USD)", "notas": "Notas"})
            st.dataframe(df_vis.style.format({"MXN Gastados": "${:,.2f}", "T.C. MXN/USD": "{:.4f}",
                "USD Equiv.": "${:,.4f}", "BTC Adquiridos": "{:.8f}", "Precio BTC (USD)": "${:,.2f}"}),
                width="stretch", hide_index=True)
            st.download_button("Descargar historial CSV", df.to_csv(index=False).encode("utf-8"), "dca_bitcoin_historial.csv", "text/csv", width="stretch")
            st.markdown("---")
            with st.expander("Editar un registro"):
                ids_edit = df["id"].tolist()
                id_editar = st.selectbox("ID del registro a editar", options=ids_edit,
                    format_func=lambda x: f"ID {x} — {df.loc[df['id']==x, 'fecha'].values[0]}", key="sel_id_editar")
                fila = df.loc[df["id"] == id_editar].iloc[0]
                if st.session_state.get("_edit_id_actual") != id_editar:
                    st.session_state["_edit_id_actual"] = id_editar
                    st.session_state["edit_mxn"] = float(fila["mxn_gastado"])
                    st.session_state["edit_tc"] = float(fila["tipo_cambio_mxn_usd"])
                    st.session_state["edit_btc"] = float(fila["btc_adquirido"])
                    st.session_state["edit_precio_usd"] = float(fila["precio_compra_btc_usd"])
                    st.session_state["edit_notas"] = fila["notas"] if fila["notas"] is not None else ""
                col_e1, col_e2 = st.columns(2, gap="large")
                with col_e1:
                    mxn_edit = st.number_input("MXN gastados", min_value=0.0, step=100.0, format="%.2f", key="edit_mxn")
                    tc_edit = st.number_input("Tipo de cambio (MXN por 1 USD)", min_value=0.01, step=0.01, format="%.4f", key="edit_tc")
                    st.info(f"USD equivalente: **${mxn_edit / tc_edit if tc_edit > 0 else 0:,.4f} USD**")
                with col_e2:
                    btc_edit = st.number_input("BTC adquiridos", min_value=0.0, step=0.00001, format="%.8f", key="edit_btc")
                    precio_btc_usd_edit = st.number_input("Precio de compra BTC (USD)", min_value=0.0, step=100.0, format="%.2f", key="edit_precio_usd")
                notas_edit = st.text_area("Notas (opcional)", height=80, key="edit_notas")
                if st.button("Guardar cambios", type="primary", key="btn_guardar_edicion"):
                    if mxn_edit <= 0 or btc_edit <= 0 or precio_btc_usd_edit <= 0 or tc_edit <= 0:
                        st.error("Todos los valores deben ser mayores a 0.")
                    else:
                        actualizar_registro_btc(int(id_editar), float(mxn_edit), float(tc_edit), float(btc_edit), float(precio_btc_usd_edit), "MXN", str(notas_edit))
                        st.session_state.pop("_edit_id_actual", None)
                        st.success("Registro actualizado.")
                        st.rerun()
            st.markdown("---")
            with st.expander("Eliminar un registro"):
                ids_disponibles = df["id"].tolist()
                id_eliminar = st.selectbox("ID del registro a eliminar", options=ids_disponibles,
                    format_func=lambda x: f"ID {x} — {df.loc[df['id']==x, 'fecha'].values[0]}")
                if st.button("Eliminar registro seleccionado", type="secondary"):
                    eliminar_registro_btc(id_eliminar)
                    st.success(f"Registro ID {id_eliminar} eliminado.")
                    st.rerun()

    with tab_resumen:
        st.subheader("Resumen de inversion BTC")
        df = obtener_registros_btc()
        if df.empty:
            st.info("No hay datos para mostrar aun.")
        else:
            total_mxn = df["mxn_gastado"].sum()
            total_usd = df["usd_equivalente"].sum()
            total_btc = df["btc_adquirido"].sum()
            precio_prom = (df["precio_compra_btc_usd"] * df["btc_adquirido"]).sum() / total_btc if total_btc > 0 else 0.0
            precio_actual = obtener_precio_btc()

            c1, c2, c3, c4, c5 = st.columns(5)
            c1.metric("Total MXN invertidos", f"${total_mxn:,.2f}")
            c2.metric("Total USD invertidos", f"${total_usd:,.2f}")
            c3.metric("BTC acumulados", f"{total_btc:.8f}")
            c4.metric("Precio promedio (USD)", f"${precio_prom:,.2f}")
            c5.metric("Numero de compras", str(len(df)))

            if precio_actual:
                valor_actual_usd = total_btc * precio_actual
                ganancia_usd = valor_actual_usd - total_usd
                pct_cambio = (ganancia_usd / total_usd * 100) if total_usd > 0 else 0
                st.markdown("---")
                st.caption(f"Precio BTC actual: **${precio_actual:,.2f} USD** (CoinGecko, cache 2 min)")
                ca, cb, cc = st.columns(3)
                ca.metric("Valor actual BTC (USD)", f"${valor_actual_usd:,.2f}", f"{ganancia_usd:+,.2f} USD")
                if tc_api:
                    cb.metric("Valor actual BTC (MXN)", f"${valor_actual_usd * tc_api:,.2f}")
                cc.metric("Rendimiento", f"{pct_cambio:+.2f}%")

            df_fees_res = obtener_fees()
            if not df_fees_res.empty:
                total_btc_fees = df_fees_res["btc_fee"].sum()
                st.markdown("---")
                st.markdown("##### Impacto de fees de envio")
                cf1, cf2, cf3, cf4 = st.columns(4)
                cf1.metric("BTC pagados en fees", f"{total_btc_fees:.8f}")
                cf2.metric("USD en fees", f"${df_fees_res['usd_fee'].sum():.6f}")
                cf3.metric("BTC neto (sin fees)", f"{total_btc - total_btc_fees:.8f}")
                cf4.metric("Operaciones de envio", str(len(df_fees_res)))

            st.markdown("---")
            df_ord = df.sort_values("fecha").copy()
            df_ord["btc_acumulado"] = df_ord["btc_adquirido"].cumsum()
            df_ord["usd_acumulado"] = df_ord["usd_equivalente"].cumsum()

            col_g1, col_g2 = st.columns(2)
            with col_g1:
                fig_btc = px.area(df_ord, x="fecha", y="btc_acumulado", title="BTC Acumulado",
                    labels={"fecha": "Fecha", "btc_acumulado": "BTC"}, color_discrete_sequence=["#f7931a"])
                fig_btc.update_layout(plot_bgcolor="#1e1e2e", paper_bgcolor="#1e1e2e", font_color="#e0e0e0")
                st.plotly_chart(fig_btc, width="stretch")
            with col_g2:
                fig_mxn = px.bar(df_ord, x="fecha", y="mxn_gastado", title="Inversion por compra (MXN)",
                    labels={"fecha": "Fecha", "mxn_gastado": "MXN"}, color_discrete_sequence=["#f7931a"])
                fig_mxn.update_layout(plot_bgcolor="#1e1e2e", paper_bgcolor="#1e1e2e", font_color="#e0e0e0")
                st.plotly_chart(fig_mxn, width="stretch")

            fig_precio = go.Figure()
            fig_precio.add_trace(go.Scatter(x=df_ord["fecha"], y=df_ord["precio_compra_btc_usd"],
                mode="lines+markers", name="Precio de compra", line=dict(color="#f7931a"), marker=dict(size=8)))
            fig_precio.add_hline(y=precio_prom, line_dash="dash", line_color="#aaaaaa",
                annotation_text=f"Precio promedio: ${precio_prom:,.2f}", annotation_font_color="#aaaaaa")
            if precio_actual:
                fig_precio.add_hline(y=precio_actual, line_dash="dot", line_color="#00d4aa",
                    annotation_text=f"Precio actual: ${precio_actual:,.2f}", annotation_font_color="#00d4aa")
            fig_precio.update_layout(title="Precio BTC en cada compra (USD)", xaxis_title="Fecha", yaxis_title="Precio (USD)",
                plot_bgcolor="#1e1e2e", paper_bgcolor="#1e1e2e", font_color="#e0e0e0")
            st.plotly_chart(fig_precio, width="stretch")

            fig_usd = px.area(df_ord, x="fecha", y="usd_acumulado", title="Capital acumulado invertido (USD)",
                labels={"fecha": "Fecha", "usd_acumulado": "USD invertidos"}, color_discrete_sequence=["#00d4aa"])
            fig_usd.update_layout(plot_bgcolor="#1e1e2e", paper_bgcolor="#1e1e2e", font_color="#e0e0e0")
            st.plotly_chart(fig_usd, width="stretch")


else:
    st.title("Panel de Registro DCA — Solana (SOL)")
    st.markdown("Registra tus compras de Solana y lleva el control de tu estrategia DCA.")
    st.markdown("---")

    precio_sol_actual = obtener_precio_sol()

    tab_reg_sol, tab_hist_sol, tab_res_sol = st.tabs(["Registrar Compra", "Historial", "Resumen & Graficas"])

    with tab_reg_sol:
        st.subheader("Nueva compra de Solana")

        if tc_api:
            st.success(f"Tipo de cambio: **1 USD = {tc_api:.4f} MXN** (actualizacion cada 5 min)")
        else:
            st.warning("No se pudo obtener el tipo de cambio. Ingresa el valor manualmente.")

        if precio_sol_actual:
            st.info(f"Precio SOL actual: **${precio_sol_actual:,.2f} USD** (CoinGecko, cache 2 min)")

        col_izq, col_der = st.columns(2, gap="large")

        with col_izq:
            st.markdown("#### Datos en pesos MXN")
            tipo_cambio_sol = st.number_input(
                "Tipo de cambio (MXN por 1 USD)",
                min_value=0.01,
                value=float(tc_api) if tc_api else 17.50,
                step=0.01,
                format="%.4f",
                key="sol_tc",
            )
            moneda_gasto_sol = st.radio(
                "Registrar gasto en:",
                options=["MXN", "USD (USDC/USDT)"],
                horizontal=True,
                key="sol_moneda_gasto",
            )
            if moneda_gasto_sol == "MXN":
                mxn_gastado_sol = st.number_input("MXN gastados", min_value=0.0, value=0.0, step=100.0, format="%.2f", key="sol_mxn")
                usd_equiv_sol = mxn_gastado_sol / tipo_cambio_sol if tipo_cambio_sol > 0 else 0.0
            else:
                usd_input_sol = st.number_input("USD gastados (USDC/USDT)", min_value=0.0, value=0.0, step=5.0, format="%.4f", key="sol_usd_input")
                usd_equiv_sol = float(usd_input_sol)
                mxn_gastado_sol = usd_input_sol * tipo_cambio_sol if tipo_cambio_sol > 0 else 0.0
                if usd_input_sol > 0:
                    st.info(f"Equivalente en MXN: **${mxn_gastado_sol:,.2f} MXN**")
            st.info(f"Equivalente en USD: **${usd_equiv_sol:,.4f} USD**" if usd_equiv_sol > 0 else "Ingresa el monto.")

        with col_der:
            st.markdown("#### Datos Solana")
            sol_adquirido = st.number_input("SOL adquiridos", min_value=0.0, value=0.0, step=0.01, format="%.6f", key="sol_cantidad")
            modo_precio_sol = st.radio("Ingresar precio SOL en:", options=["MXN", "USD"], horizontal=True, key="sol_modo_precio")
            if modo_precio_sol == "MXN":
                precio_sol_mxn_input = st.number_input("Precio de compra SOL (MXN)", min_value=0.0, value=0.0, step=10.0, format="%.2f", key="sol_precio_mxn")
                precio_sol_usd = precio_sol_mxn_input / tipo_cambio_sol if tipo_cambio_sol > 0 and precio_sol_mxn_input > 0 else 0.0
                if precio_sol_mxn_input > 0:
                    st.info(f"Equivalente en USD: **${precio_sol_usd:,.2f} USD**")
            else:
                precio_sol_usd = st.number_input("Precio de compra SOL (USD)", min_value=0.0,
                    value=float(precio_sol_actual) if precio_sol_actual else 0.0, step=1.0, format="%.2f", key="sol_precio_usd")
                if precio_sol_usd > 0:
                    st.info(f"Equivalente en MXN: **${precio_sol_usd * tipo_cambio_sol:,.2f} MXN**")

        notas_sol = st.text_area("Notas (opcional)", placeholder="Ej: Compra en Bitso, semana 5...", height=80, key="sol_notas")

        if mxn_gastado_sol > 0 or sol_adquirido > 0:
            st.markdown("---")
            st.markdown("**Vista previa:**")
            p1, p2, p3, p4 = st.columns(4)
            p1.metric("MXN gastados", f"${mxn_gastado_sol:,.2f}")
            p2.metric("USD equivalente", f"${usd_equiv_sol:,.4f}")
            p3.metric("SOL adquiridos", f"{sol_adquirido:.6f}")
            p4.metric("Precio SOL (USD)", f"${precio_sol_usd:,.2f}")

        st.markdown("---")
        if st.button("Guardar registro SOL", type="primary", width="stretch", key="btn_save_sol"):
            errores_sol = []
            if usd_equiv_sol <= 0:
                errores_sol.append("El monto gastado debe ser mayor a 0.")
            if sol_adquirido <= 0:
                errores_sol.append("La cantidad de SOL debe ser mayor a 0.")
            if precio_sol_usd <= 0:
                errores_sol.append("El precio de compra del SOL debe ser mayor a 0.")
            if errores_sol:
                for e in errores_sol:
                    st.error(e)
            else:
                insertar_registro_sol(mxn_gastado_sol, tipo_cambio_sol, usd_equiv_sol, sol_adquirido, precio_sol_usd,
                                      "USD" if moneda_gasto_sol != "MXN" else "MXN", notas_sol)
                st.success("Registro SOL guardado exitosamente!")
                st.balloons()

    with tab_hist_sol:
        st.subheader("Historial de compras SOL")
        df_sol = obtener_registros_sol()
        if df_sol.empty:
            st.info("No hay registros de Solana aun.")
        else:
            df_sol_vis = df_sol.rename(columns={"id": "ID", "fecha": "Fecha (CDMX)", "mxn_gastado": "MXN Gastados",
                "tipo_cambio_mxn_usd": "T.C. MXN/USD", "usd_equivalente": "USD Equiv.",
                "sol_adquirido": "SOL Adquiridos", "precio_compra_sol_usd": "Precio SOL (USD)", "notas": "Notas"})
            st.dataframe(df_sol_vis.style.format({"MXN Gastados": "${:,.2f}", "T.C. MXN/USD": "{:.4f}",
                "USD Equiv.": "${:,.4f}", "SOL Adquiridos": "{:.6f}", "Precio SOL (USD)": "${:,.2f}"}),
                width="stretch", hide_index=True)
            st.download_button("Descargar historial SOL CSV", df_sol.to_csv(index=False).encode("utf-8"),
                "dca_solana_historial.csv", "text/csv", width="stretch")
            st.markdown("---")
            with st.expander("Eliminar un registro SOL"):
                ids_sol = df_sol["id"].tolist()
                id_sol_del = st.selectbox("ID del registro a eliminar", options=ids_sol,
                    format_func=lambda x: f"ID {x} — {df_sol.loc[df_sol['id']==x, 'fecha'].values[0]}", key="sel_sol_del")
                if st.button("Eliminar registro SOL seleccionado", type="secondary", key="btn_del_sol"):
                    eliminar_registro_sol(id_sol_del)
                    st.success(f"Registro ID {id_sol_del} eliminado.")
                    st.rerun()

    with tab_res_sol:
        st.subheader("Resumen de inversion SOL")
        df_sol = obtener_registros_sol()
        if df_sol.empty:
            st.info("No hay datos de Solana para mostrar aun.")
        else:
            total_mxn_sol = df_sol["mxn_gastado"].sum()
            total_usd_sol = df_sol["usd_equivalente"].sum()
            total_sol = df_sol["sol_adquirido"].sum()
            precio_prom_sol = (df_sol["precio_compra_sol_usd"] * df_sol["sol_adquirido"]).sum() / total_sol if total_sol > 0 else 0.0

            c1, c2, c3, c4, c5 = st.columns(5)
            c1.metric("Total MXN invertidos", f"${total_mxn_sol:,.2f}")
            c2.metric("Total USD invertidos", f"${total_usd_sol:,.2f}")
            c3.metric("SOL acumulados", f"{total_sol:.6f}")
            c4.metric("Precio promedio (USD)", f"${precio_prom_sol:,.2f}")
            c5.metric("Numero de compras", str(len(df_sol)))

            if precio_sol_actual:
                valor_actual_usd_sol = total_sol * precio_sol_actual
                ganancia_usd_sol = valor_actual_usd_sol - total_usd_sol
                pct_cambio_sol = (ganancia_usd_sol / total_usd_sol * 100) if total_usd_sol > 0 else 0
                st.markdown("---")
                st.caption(f"Precio SOL actual: **${precio_sol_actual:,.2f} USD** (CoinGecko, cache 2 min)")
                ca, cb, cc = st.columns(3)
                ca.metric("Valor actual SOL (USD)", f"${valor_actual_usd_sol:,.2f}", f"{ganancia_usd_sol:+,.2f} USD")
                if tc_api:
                    cb.metric("Valor actual SOL (MXN)", f"${valor_actual_usd_sol * tc_api:,.2f}")
                cc.metric("Rendimiento", f"{pct_cambio_sol:+.2f}%")

            st.markdown("---")
            df_sol_ord = df_sol.sort_values("fecha").copy()
            df_sol_ord["sol_acumulado"] = df_sol_ord["sol_adquirido"].cumsum()
            df_sol_ord["usd_acumulado"] = df_sol_ord["usd_equivalente"].cumsum()

            col_g1, col_g2 = st.columns(2)
            with col_g1:
                fig_sol = px.area(df_sol_ord, x="fecha", y="sol_acumulado", title="SOL Acumulado",
                    labels={"fecha": "Fecha", "sol_acumulado": "SOL"}, color_discrete_sequence=["#9945FF"])
                fig_sol.update_layout(plot_bgcolor="#1e1e2e", paper_bgcolor="#1e1e2e", font_color="#e0e0e0")
                st.plotly_chart(fig_sol, width="stretch")
            with col_g2:
                fig_mxn_sol = px.bar(df_sol_ord, x="fecha", y="mxn_gastado", title="Inversion por compra SOL (MXN)",
                    labels={"fecha": "Fecha", "mxn_gastado": "MXN"}, color_discrete_sequence=["#9945FF"])
                fig_mxn_sol.update_layout(plot_bgcolor="#1e1e2e", paper_bgcolor="#1e1e2e", font_color="#e0e0e0")
                st.plotly_chart(fig_mxn_sol, width="stretch")

            fig_precio_sol = go.Figure()
            fig_precio_sol.add_trace(go.Scatter(x=df_sol_ord["fecha"], y=df_sol_ord["precio_compra_sol_usd"],
                mode="lines+markers", name="Precio de compra", line=dict(color="#9945FF"), marker=dict(size=8)))
            fig_precio_sol.add_hline(y=precio_prom_sol, line_dash="dash", line_color="#aaaaaa",
                annotation_text=f"Precio promedio: ${precio_prom_sol:,.2f}", annotation_font_color="#aaaaaa")
            if precio_sol_actual:
                fig_precio_sol.add_hline(y=precio_sol_actual, line_dash="dot", line_color="#00d4aa",
                    annotation_text=f"Precio actual: ${precio_sol_actual:,.2f}", annotation_font_color="#00d4aa")
            fig_precio_sol.update_layout(title="Precio SOL en cada compra (USD)", xaxis_title="Fecha", yaxis_title="Precio (USD)",
                plot_bgcolor="#1e1e2e", paper_bgcolor="#1e1e2e", font_color="#e0e0e0")
            st.plotly_chart(fig_precio_sol, width="stretch")

            fig_usd_sol = px.area(df_sol_ord, x="fecha", y="usd_acumulado", title="Capital acumulado invertido en SOL (USD)",
                labels={"fecha": "Fecha", "usd_acumulado": "USD invertidos"}, color_discrete_sequence=["#00d4aa"])
            fig_usd_sol.update_layout(plot_bgcolor="#1e1e2e", paper_bgcolor="#1e1e2e", font_color="#e0e0e0")
            st.plotly_chart(fig_usd_sol, width="stretch")


def _running_under_streamlit():
    try:
        from streamlit.runtime import exists
        return exists()
    except Exception:
        return False


if __name__ == "__main__" and not _running_under_streamlit():
    script = os.path.abspath(__file__)
    streamlit_bin = os.path.join(os.path.dirname(sys.executable), "streamlit")
    if sys.platform == "win32":
        streamlit_bin = streamlit_bin + ".exe"
    subprocess.run([streamlit_bin, "run", script] + sys.argv[1:], check=True)

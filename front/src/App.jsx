import { HashRouter, Routes, Route } from "react-router-dom";
import WelcomePage from "./pages/WelcomePage";
import CitiesPage from "./pages/CitiesPage";
import CoordinatesPage from "./pages/CoordinatesPage";
import HumansPage from "./pages/HumansPage";
import FeaturePage from "./pages/FeaturePage";
import AppHeader from "./components/AppHeader";
import Snowfall from "react-snowfall";
import ImportPage from "./pages/ImportPage";

export default function App() {
  return (
    <HashRouter>
      <Snowfall
        snowflakeCount={120}
        speed={[0.6, 1.4]}
        wind={[-0.3, 0.8]}
        radius={[0.8, 2.5]}
      />

      <AppHeader />

      <Routes>
        <Route path="/" element={<WelcomePage />} />
        <Route path="/cities" element={<CitiesPage />} />
        <Route path="/coordinates" element={<CoordinatesPage />} />
        <Route path="/humans" element={<HumansPage />} />
        <Route path="/features" element={<FeaturePage />} />
        <Route path="/imports" element={<ImportPage />} />
      </Routes>
    </HashRouter>
  );
}
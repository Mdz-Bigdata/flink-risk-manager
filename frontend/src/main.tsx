import React from 'react';
import ReactDOM from 'react-dom/client';
import { ConfigProvider, theme } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import App from './App';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider
      locale={zhCN}
      theme={{
        algorithm: theme.defaultAlgorithm,
        token: {
          colorPrimary: '#2563eb',
          colorInfo: '#2563eb',
          colorSuccess: '#10b981',
          colorWarning: '#f59e0b',
          colorError: '#ef4444',
          borderRadius: 12,
          borderRadiusLG: 16,
          fontFamily: "'Outfit', -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Segoe UI', sans-serif",
          colorBgContainer: '#ffffff',
          colorBgElevated: '#ffffff',
          colorBorderSecondary: '#e2e8f0',
          colorText: '#1e293b',
          colorTextSecondary: '#64748b',
          fontSize: 14,
          controlHeight: 36,
        },
        components: {
          Layout: {
            siderBg: '#ffffff',
            headerBg: '#ffffff',
            bodyBg: '#f8fafc',
            triggerBg: '#f1f5f9',
            triggerColor: '#64748b',
          },
          Menu: {
            itemBg: 'transparent',
            itemSelectedBg: '#eff6ff',
            itemSelectedColor: '#2563eb',
            itemHoverBg: '#f1f5f9',
            itemHoverColor: '#2563eb',
            itemColor: '#64748b',
            itemBorderRadius: 10,
            itemHeight: 44,
            iconSize: 18,
            iconMarginInlineEnd: 12,
          },
          Card: {
            colorBgContainer: '#ffffff',
            borderRadiusLG: 16,
            boxShadow: '0 1px 3px rgba(0,0,0,0.04), 0 1px 2px rgba(0,0,0,0.02)',
            boxShadowSecondary: '0 4px 24px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.02)',
          },
          Table: {
            colorBgContainer: '#ffffff',
            headerBg: '#f8fafc',
            headerColor: '#475569',
            borderRadius: 12,
          },
          Button: {
            borderRadius: 10,
            borderRadiusLG: 12,
          },
          Input: {
            borderRadius: 10,
          },
          Select: {
            borderRadius: 10,
          },
          Modal: {
            borderRadiusLG: 20,
          },
          Tag: {
            borderRadiusSM: 6,
          },
        },
      }}
    >
      <App />
    </ConfigProvider>
  </React.StrictMode>,
);

import { createStyles } from 'antd-style';

export const useStyles = createStyles(({ token, css }) => ({
  card: css`
    border-radius: ${token.borderRadiusLG}px;
    border: 1px solid #f1f5f9;
    transition: box-shadow 0.25s cubic-bezier(0.4, 0, 0.2, 1),
                transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
    &:hover {
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08), 0 2px 8px rgba(0, 0, 0, 0.04);
      transform: translateY(-2px);
    }
    .ant-statistic-title {
      color: #64748b !important;
      font-size: 13px;
      font-weight: 500;
      letter-spacing: 0.3px;
      text-transform: uppercase;
    }
    .ant-statistic-content-value {
      font-size: 32px;
      font-weight: 700;
      font-family: 'Outfit', sans-serif;
      letter-spacing: -0.5px;
      color: #1e293b;
    }
  `,
  content: css`
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
  `,
  info: css`
    flex: 1;
    min-width: 0;
  `,
  icon: css`
    width: 52px;
    height: 52px;
    border-radius: 14px;
    background: #eff6ff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    color: #2563eb;
    flex-shrink: 0;
    transition: all 0.2s ease;
  `,
}));

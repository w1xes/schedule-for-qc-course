import React from 'react';
import { render, screen } from '@testing-library/react';
import { getHref } from './getHref';

describe('getHref function', () => {
    describe('happy path — valid link', () => {
        it('should render an anchor element with correct href', () => {
            const link = 'https://www.youtube.com/';
            render(getHref(link));
            const anchor = screen.getByTitle(link);
            expect(anchor).toBeInTheDocument();
            expect(anchor).toHaveAttribute('href', link);
        });

        it('should have correct class name', () => {
            const link = 'https://www.youtube.com/';
            render(getHref(link));
            const anchor = screen.getByTitle(link);
            expect(anchor).toHaveClass('link-to-meeting');
        });

        it('should open in new tab (target="_blank")', () => {
            const link = 'https://www.youtube.com/';
            render(getHref(link));
            const anchor = screen.getByTitle(link);
            expect(anchor).toHaveAttribute('target', '_blank');
        });

        it('should have rel="noreferrer" for security', () => {
            const link = 'https://www.youtube.com/';
            render(getHref(link));
            const anchor = screen.getByTitle(link);
            expect(anchor).toHaveAttribute('rel', 'noreferrer');
        });
    });

    describe('edge cases', () => {
        it('should render with empty string link', () => {
            const link = '';
            render(getHref(link));
            const anchor = screen.getByTitle(link);
            expect(anchor).toBeInTheDocument();
            expect(anchor).toHaveAttribute('href', '');
        });

        it('should render with null link (href becomes empty)', () => {
            render(getHref(null));
            // When title is null, React renders without a title attribute; query by role
            const anchor = document.querySelector('a.link-to-meeting');
            expect(anchor).toBeInTheDocument();
        });

        it('should render with link that has no protocol', () => {
            const link = 'www.example.com';
            render(getHref(link));
            const anchor = screen.getByTitle(link);
            expect(anchor).toBeInTheDocument();
            expect(anchor).toHaveAttribute('href', link);
        });

        it('should render with a very long link', () => {
            const link = 'https://www.example.com/' + 'a'.repeat(2000);
            render(getHref(link));
            const anchor = screen.getByTitle(link);
            expect(anchor).toBeInTheDocument();
            expect(anchor).toHaveAttribute('href', link);
        });

        it('should render with link containing query params and fragments', () => {
            const link = 'https://www.example.com/path?foo=bar&baz=qux#section';
            render(getHref(link));
            const anchor = screen.getByTitle(link);
            expect(anchor).toBeInTheDocument();
            expect(anchor).toHaveAttribute('href', link);
        });
    });
});